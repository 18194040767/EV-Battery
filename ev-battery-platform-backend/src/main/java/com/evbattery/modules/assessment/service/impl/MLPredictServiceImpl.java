package com.evbattery.modules.assessment.service.impl;

import com.evbattery.modules.assessment.service.MLPredictService;
import com.evbattery.modules.battery.entity.BatteryRecord;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class MLPredictServiceImpl implements MLPredictService {

    private static final Logger log = LoggerFactory.getLogger(MLPredictServiceImpl.class);

    @Value("${ml.python-path:python}")
    private String pythonPath;

    @Value("${ml.script-path:../ml/predict.py}")
    private String scriptPath;

    @Value("${ml.timeout-seconds:5}")
    private int timeoutSeconds;

    @Value("${ml.retry-count:1}")
    private int retryCount;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public Integer predictSoh(BatteryRecord batteryRecord) {
        RuntimeException latest = null;
        int totalAttempts = Math.max(1, retryCount + 1);
        for (int i = 1; i <= totalAttempts; i++) {
            try {
                return doPredict(batteryRecord);
            } catch (RuntimeException ex) {
                latest = ex;
                log.warn("ML prediction attempt {}/{} failed", i, totalAttempts, ex);
            }
        }
        throw latest == null ? new RuntimeException("机器学习预测失败") : latest;
    }

    private Integer doPredict(BatteryRecord batteryRecord) {
        try {
            Path absoluteScriptPath = Paths.get(scriptPath).toAbsolutePath().normalize();
            List<String> command = new ArrayList<String>();
            command.add(pythonPath);
            command.add(absoluteScriptPath.toString());
            command.add("--cap");
            command.add(decimalText(batteryRecord.getCapacityRetentionRate()));
            command.add("--ir");
            command.add(decimalText(batteryRecord.getInternalResistanceRatio()));
            command.add("--cycle");
            command.add(String.valueOf(batteryRecord.getCycleCount()));
            command.add("--temp");
            command.add(decimalText(batteryRecord.getAvgTemperature()));
            log.info("Executing command: {}", command);

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            File workingDirectory = absoluteScriptPath.getParent() == null ? null : absoluteScriptPath.getParent().toFile();
            if (workingDirectory != null) {
                processBuilder.directory(workingDirectory);
            }
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            StringBuilder outputBuilder = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                outputBuilder.append(line);
            }

            boolean finished = process.waitFor(Math.max(1, timeoutSeconds), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("Python prediction timeout after " + timeoutSeconds + "s");
            }
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new IllegalStateException("Python prediction failed: " + outputBuilder);
            }

            JsonNode jsonNode = objectMapper.readTree(outputBuilder.toString());
            return jsonNode.path("predicted_soh").asInt();
        } catch (Exception ex) {
            throw new RuntimeException("机器学习预测失败: " + ex.getMessage(), ex);
        }
    }

    private String decimalText(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }
}
