package com.evbattery.modules.assessment.service.impl;

import com.evbattery.modules.assessment.service.MLPredictService;
import com.evbattery.modules.battery.entity.BatteryRecord;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Service
public class MLPredictServiceImpl implements MLPredictService {

    private static final Logger log = LoggerFactory.getLogger(MLPredictServiceImpl.class);

    @Value("${ml.python-path:python}")
    private String pythonPath;

    @Value("${ml.script-path:../ml/predict.py}")
    private String scriptPath;

    @Value("${ml.timeout-seconds:20}")
    private int timeoutSeconds;

    @Value("${ml.startup-timeout-seconds:45}")
    private int startupTimeoutSeconds;

    @Value("${ml.retry-count:1}")
    private int retryCount;

    @Value("${ml.warmup:true}")
    private boolean warmup;

    @Resource
    private ObjectMapper objectMapper;

    private Process process;
    private BufferedWriter processInput;
    private LinkedBlockingQueue<String> processOutput = new LinkedBlockingQueue<String>();

    @PostConstruct
    public void warmupPredictor() {
        if (!warmup) {
            return;
        }
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ensureProcess();
                } catch (RuntimeException ex) {
                    log.warn("ML predictor warmup failed; it will retry on demand", ex);
                }
            }
        }, "ml-predictor-warmup");
        thread.setDaemon(true);
        thread.start();
    }

    @PreDestroy
    public synchronized void shutdownPredictor() {
        stopProcess();
    }

    @Override
    public Integer predictSoh(BatteryRecord batteryRecord) {
        RuntimeException latest = null;
        int totalAttempts = Math.max(1, retryCount + 1);
        for (int i = 1; i <= totalAttempts; i++) {
            try {
                return predictWithServer(batteryRecord);
            } catch (RuntimeException ex) {
                latest = ex;
                log.warn("ML prediction attempt {}/{} failed", i, totalAttempts, ex);
                stopProcess();
            }
        }
        throw latest == null ? new RuntimeException("机器学习预测失败") : latest;
    }

    private synchronized Integer predictWithServer(BatteryRecord batteryRecord) {
        try {
            ensureProcess();
            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("cap", decimalText(batteryRecord.getCapacityRetentionRate()));
            payload.put("ir", decimalText(batteryRecord.getInternalResistanceRatio()));
            payload.put("cycle", batteryRecord.getCycleCount() == null ? 0 : batteryRecord.getCycleCount());
            payload.put("temp", decimalText(batteryRecord.getAvgTemperature()));

            processInput.write(objectMapper.writeValueAsString(payload));
            processInput.newLine();
            processInput.flush();

            String output = processOutput.poll(Math.max(1, timeoutSeconds), TimeUnit.SECONDS);
            if (output == null) {
                throw new IllegalStateException("Python prediction timeout after " + timeoutSeconds + "s");
            }

            JsonNode jsonNode = objectMapper.readTree(output);
            if (jsonNode.hasNonNull("error")) {
                throw new IllegalStateException("Python prediction failed: " + jsonNode.path("error").asText());
            }
            if (!jsonNode.has("predicted_soh")) {
                throw new IllegalStateException("Unexpected Python prediction response: " + output);
            }
            return jsonNode.path("predicted_soh").asInt();
        } catch (Exception ex) {
            throw new RuntimeException("机器学习预测失败: " + ex.getMessage(), ex);
        }
    }

    private synchronized void ensureProcess() {
        if (process != null && process.isAlive() && processInput != null) {
            return;
        }
        stopProcess();
        startProcess();
    }

    private void startProcess() {
        try {
            Path absoluteScriptPath = Paths.get(scriptPath).toAbsolutePath().normalize();
            List<String> command = new ArrayList<String>();
            command.add(pythonPath);
            command.add(absoluteScriptPath.toString());
            command.add("--serve");
            log.info("Starting ML predictor server: {}", command);

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            File workingDirectory = absoluteScriptPath.getParent() == null ? null : absoluteScriptPath.getParent().toFile();
            if (workingDirectory != null) {
                processBuilder.directory(workingDirectory);
            }
            process = processBuilder.start();
            processInput = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
            processOutput = new LinkedBlockingQueue<String>();
            startStreamReader(process.getInputStream(), processOutput, "ml-predictor-output", false);
            startStreamReader(process.getErrorStream(), null, "ml-predictor-error", true);

            String ready = processOutput.poll(Math.max(1, startupTimeoutSeconds), TimeUnit.SECONDS);
            if (ready == null) {
                throw new IllegalStateException("Python predictor startup timeout after " + startupTimeoutSeconds + "s");
            }
            JsonNode jsonNode = objectMapper.readTree(ready);
            if (!"ready".equals(jsonNode.path("status").asText())) {
                throw new IllegalStateException("Unexpected Python predictor startup response: " + ready);
            }
            log.info("ML predictor server is ready");
        } catch (Exception ex) {
            stopProcess();
            throw new RuntimeException("机器学习预测服务启动失败: " + ex.getMessage(), ex);
        }
    }

    private void startStreamReader(final java.io.InputStream inputStream,
                                   final LinkedBlockingQueue<String> queue,
                                   String threadName,
                                   final boolean errorStream) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (queue != null) {
                            queue.offer(line);
                        } else if (errorStream && !line.trim().isEmpty()) {
                            log.warn("ML predictor stderr: {}", line);
                        }
                    }
                } catch (Exception ex) {
                    if (errorStream) {
                        log.debug("ML predictor stderr reader stopped", ex);
                    } else {
                        log.warn("ML predictor output reader stopped", ex);
                    }
                }
            }
        }, threadName);
        thread.setDaemon(true);
        thread.start();
    }

    private synchronized void stopProcess() {
        if (processInput != null) {
            try {
                processInput.close();
            } catch (Exception ignored) {
            }
            processInput = null;
        }
        if (process != null) {
            try {
                process.destroy();
                if (!process.waitFor(2, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
            } catch (Exception ignored) {
                process.destroyForcibly();
            }
            process = null;
        }
        processOutput.clear();
    }

    private String decimalText(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }
}
