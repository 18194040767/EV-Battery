package com.evbattery.modules.assessment.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.evbattery.common.AuthUserContext;
import com.evbattery.common.result.Result;
import com.evbattery.modules.assessment.entity.HealthAssessment;
import com.evbattery.modules.assessment.mapper.HealthAssessmentMapper;
import com.evbattery.modules.assessment.service.LLMService;
import com.evbattery.modules.assessment.service.MLPredictService;
import com.evbattery.modules.assessment.service.RuleScoreService;
import com.evbattery.modules.assessment.vo.HealthAssessmentVO;
import com.evbattery.modules.battery.entity.BatteryRecord;
import com.evbattery.modules.battery.mapper.BatteryRecordMapper;
import com.evbattery.modules.report.entity.Report;
import com.evbattery.modules.report.mapper.ReportMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/assessment")
public class AssessmentController {

    private final Map<String, BatchAssessmentTask> batchTasks = new ConcurrentHashMap<String, BatchAssessmentTask>();

    @Resource
    private BatteryRecordMapper batteryRecordMapper;
    @Resource
    private HealthAssessmentMapper healthAssessmentMapper;
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private RuleScoreService ruleScoreService;
    @Resource
    private MLPredictService mlPredictService;
    @Resource
    private LLMService llmService;
    @Resource
    private ReportMapper reportMapper;
    @Resource
    private JdbcTemplate jdbcTemplate;

    @PostMapping("/trigger")
    public Result<HealthAssessmentVO> trigger(@RequestParam Long batteryId,
                                              @RequestParam(defaultValue = "false") Boolean useML) {
        try {
            BatteryRecord battery = batteryRecordMapper.selectById(batteryId);
            if (battery == null || Boolean.TRUE.equals(battery.getIsDeleted())) {
                return Result.fail(404, "Battery record not found");
            }

            enrichBatteryFeaturesFromRecord(battery);
            List<String> missingFields = collectMissingAssessmentFields(battery);
            if (!missingFields.isEmpty()) {
                return Result.fail(400, "档案评估数据不完整，请先补充: " + String.join("、", missingFields));
            }

            int ruleScore = ruleScoreService.calculateScore(
                    decimalValue(battery.getCapacityRetentionRate()),
                    decimalValue(battery.getInternalResistanceRatio()),
                    battery.getCycleCount(),
                    decimalValue(battery.getAvgTemperature())
            );
            Integer mlScore = null;
            int finalScore = ruleScore;
            String responseMessage = "success";
            if (Boolean.TRUE.equals(useML)) {
                try {
                    mlScore = mlPredictService.predictSoh(battery);
                    finalScore = (int) Math.round((ruleScore + mlScore) / 2.0D);
                } catch (RuntimeException mlEx) {
                    responseMessage = "机器学习服务暂时不可用，已降级为规则评分";
                }
            }

            String healthLevel = ruleScoreService.resolveHealthLevel(finalScore);
            String suggestedScene = ruleScoreService.resolveSuggestedScene(healthLevel);
            List<Map<String, Object>> trendData = generateTrendData(decimalValue(battery.getCapacityRetentionRate()));
            String llmSummary = llmService.generateSummary(finalScore, healthLevel, suggestedScene, battery);

            HealthAssessment assessment = new HealthAssessment();
            assessment.setBatteryId(batteryId);
            assessment.setHealthScore(finalScore);
            assessment.setHealthLevel(healthLevel);
            assessment.setRuleScore(ruleScore);
            assessment.setMlScore(mlScore);
            assessment.setSuggestedScene(suggestedScene);
            assessment.setTrendData(objectMapper.writeValueAsString(trendData));
            assessment.setLlmSummary(llmSummary);
            assessment.setAssessmentTime(LocalDateTime.now());
            assessment.setIsMlEnhanced(Boolean.TRUE.equals(useML));
            healthAssessmentMapper.insert(assessment);
            battery.setStatus("ASSESSED");
            batteryRecordMapper.updateById(battery);
            createAssessmentReport(assessment, battery);
            pushAssessmentMessage(assessment, battery, responseMessage);

            return Result.success(responseMessage, toVO(assessment, battery));
        } catch (Exception ex) {
            return Result.fail(500, "Assessment failed: " + ex.getMessage());
        }
    }

    @GetMapping("/battery/{batteryId}/latest")
    public Result<HealthAssessmentVO> latest(@PathVariable Long batteryId) {
        HealthAssessment assessment = healthAssessmentMapper.selectOne(new LambdaQueryWrapper<HealthAssessment>()
                .eq(HealthAssessment::getBatteryId, batteryId)
                .orderByDesc(HealthAssessment::getAssessmentTime)
                .orderByDesc(HealthAssessment::getId)
                .last("limit 1"));
        if (assessment == null) {
            return Result.fail(404, "No assessment found");
        }
        return Result.success(toVO(assessment, batteryRecordMapper.selectById(batteryId)));
    }

    @GetMapping("/battery/{batteryId}/history")
    public Result<List<HealthAssessmentVO>> history(@PathVariable Long batteryId) {
        List<HealthAssessment> assessments = healthAssessmentMapper.selectList(new LambdaQueryWrapper<HealthAssessment>()
                .eq(HealthAssessment::getBatteryId, batteryId)
                .orderByDesc(HealthAssessment::getAssessmentTime)
                .orderByDesc(HealthAssessment::getId));
        BatteryRecord batteryRecord = batteryRecordMapper.selectById(batteryId);
        List<HealthAssessmentVO> result = new ArrayList<HealthAssessmentVO>();
        for (HealthAssessment assessment : assessments) {
            result.add(toVO(assessment, batteryRecord));
        }
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<HealthAssessmentVO> detail(@PathVariable Long id) {
        HealthAssessment assessment = healthAssessmentMapper.selectById(id);
        if (assessment == null) {
            return Result.fail(404, "Assessment not found");
        }
        return Result.success(toVO(assessment, batteryRecordMapper.selectById(assessment.getBatteryId())));
    }

    @PostMapping("/batch/trigger")
    public Result<Map<String, Object>> triggerBatch(@RequestBody Map<String, Object> body) {
        List<Long> batteryIds = objectMapper.convertValue(body.get("batteryIds"), new TypeReference<List<Long>>() {});
        final Boolean useML = body.get("useML") == null ? Boolean.FALSE : Boolean.valueOf(String.valueOf(body.get("useML")));
        if (batteryIds == null || batteryIds.isEmpty()) {
            return Result.fail(400, "Please select batteries to assess");
        }

        final BatchAssessmentTask task = new BatchAssessmentTask(batteryIds.size());
        final String taskId = UUID.randomUUID().toString();
        batchTasks.put(taskId, task);

        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    task.setStatus("RUNNING");
                    for (Long batteryId : batteryIds) {
                        Result<HealthAssessmentVO> result = trigger(batteryId, useML);
                        if (result.getCode() == 200) {
                            task.getSuccessCount().incrementAndGet();
                            if (result.getData() != null) {
                                task.getResults().add(result.getData());
                            }
                        } else {
                            task.getFailCount().incrementAndGet();
                            task.setLastError(result.getMessage());
                            task.getErrors().add(buildTaskError(batteryId, result.getMessage()));
                        }
                        task.getCompletedCount().incrementAndGet();
                    }
                    task.setStatus(task.getFailCount().get() > 0 ? "PARTIAL_SUCCESS" : "SUCCESS");
                } catch (Throwable ex) {
                    task.setFailCount(task.getTotal() - task.getCompletedCount().get());
                    task.setStatus("FAILED");
                    task.setLastError(ex.getMessage());
                } finally {
                    task.setFinished(true);
                }
            }
        }, "assessment-batch-" + taskId);
        worker.setDaemon(true);
        worker.start();

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("taskId", taskId);
        data.put("status", task.getStatus());
        data.put("total", batteryIds.size());
        return Result.success("Batch assessment task created", data);
    }

    @GetMapping("/batch/task/{taskId}")
    public Result<Map<String, Object>> taskStatus(@PathVariable String taskId) {
        BatchAssessmentTask task = batchTasks.get(taskId);
        if (task == null) {
            return Result.fail(404, "Task not found");
        }
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("taskId", taskId);
        data.put("status", task.getStatus());
        data.put("total", task.getTotal());
        data.put("completed", task.getCompletedCount().get());
        data.put("successCount", task.getSuccessCount().get());
        data.put("failCount", task.getFailCount().get());
        data.put("finished", task.isFinished());
        data.put("lastError", task.getLastError());
        data.put("results", task.getResults());
        data.put("errors", task.getErrors());
        return Result.success(data);
    }

    @PostMapping("/dataset/quick-run")
    public Result<Map<String, Object>> quickRunDataset(@RequestParam(defaultValue = "10") Integer limit,
                                                        @RequestParam(defaultValue = "true") Boolean useML) {
        try {
            int maxCount = limit == null || limit < 1 ? 10 : Math.min(limit, 50);
            Path datasetDir = Paths.get("../dataset").toAbsolutePath().normalize();
            if (!Files.exists(datasetDir)) {
                return Result.fail(400, "dataset目录不存在");
            }
            List<Path> csvFiles = Files.list(datasetDir)
                    .filter(path -> Files.isRegularFile(path) && path.getFileName().toString().toLowerCase().endsWith(".csv"))
                    .limit(maxCount)
                    .collect(Collectors.toList());
            if (csvFiles.isEmpty()) {
                return Result.fail(400, "dataset目录下没有CSV文件");
            }
            Long userId = currentUserId();
            List<Map<String, Object>> records = new ArrayList<Map<String, Object>>();
            int successCount = 0;
            int failCount = 0;
            for (Path csvFile : csvFiles) {
                try {
                    BatteryRecord battery = createBatteryFromDataset(csvFile, userId);
                    Result<HealthAssessmentVO> result = trigger(battery.getId(), useML);
                    if (result.getCode() != 200 || result.getData() == null) {
                        failCount++;
                        continue;
                    }
                    successCount++;
                    Map<String, Object> item = new LinkedHashMap<String, Object>();
                    item.put("batteryId", battery.getId());
                    item.put("batteryCode", battery.getBatteryCode());
                    item.put("assessmentId", result.getData().getId());
                    item.put("healthScore", result.getData().getHealthScore());
                    item.put("healthLevel", result.getData().getHealthLevel());
                    records.add(item);
                } catch (Exception ex) {
                    failCount++;
                }
            }
            Map<String, Object> data = new LinkedHashMap<String, Object>();
            data.put("total", csvFiles.size());
            data.put("successCount", successCount);
            data.put("failCount", failCount);
            data.put("records", records);
            return Result.success("一键评估完成", data);
        } catch (Exception ex) {
            return Result.fail(500, "一键评估失败: " + ex.getMessage());
        }
    }

    private void enrichBatteryFeaturesFromRecord(BatteryRecord battery) throws Exception {
        boolean changed = false;
        Map<String, Object> featureMap = readFeatureMap(battery.getFeatureJson());

        if (battery.getCapacityRetentionRate() == null) {
            BigDecimal value = decimalFromFeature(featureMap, "capacityRetentionRate");
            if (value != null) {
                battery.setCapacityRetentionRate(value);
                changed = true;
            }
        }
        if (battery.getInternalResistanceRatio() == null) {
            BigDecimal value = decimalFromFeature(featureMap, "internalResistanceRatio");
            if (value != null) {
                battery.setInternalResistanceRatio(value);
                changed = true;
            }
        }
        if (battery.getCycleCount() == null) {
            Integer value = integerFromFeature(featureMap, "cycleCount");
            if (value != null) {
                battery.setCycleCount(value);
                changed = true;
            }
        }
        if (battery.getAvgTemperature() == null) {
            BigDecimal value = decimalFromFeature(featureMap, "avgTemperature");
            if (value != null) {
                battery.setAvgTemperature(value);
                changed = true;
            }
        }

        if (changed) {
            featureMap.put("capacityRetentionRate", battery.getCapacityRetentionRate());
            featureMap.put("internalResistanceRatio", battery.getInternalResistanceRatio());
            featureMap.put("cycleCount", battery.getCycleCount());
            featureMap.put("avgTemperature", battery.getAvgTemperature());
            battery.setFeatureJson(objectMapper.writeValueAsString(featureMap));
            batteryRecordMapper.updateById(battery);
        }
    }

    private Map<String, Object> readFeatureMap(String featureJson) throws Exception {
        if (featureJson == null || featureJson.trim().isEmpty()) {
            return new HashMap<String, Object>();
        }
        return objectMapper.readValue(featureJson, new TypeReference<Map<String, Object>>() {});
    }

    private BigDecimal decimalFromFeature(Map<String, Object> featureMap, String key) {
        Object value = featureMap.get(key);
        if (value == null) {
            return null;
        }
        try {
            return scale(Double.parseDouble(String.valueOf(value)));
        } catch (Exception ignored) {
            return null;
        }
    }

    private Integer integerFromFeature(Map<String, Object> featureMap, String key) {
        Object value = featureMap.get(key);
        if (value == null) {
            return null;
        }
        try {
            return (int) Math.round(Double.parseDouble(String.valueOf(value)));
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<String> collectMissingAssessmentFields(BatteryRecord battery) {
        List<String> fields = new ArrayList<String>();
        if (battery.getCapacityRetentionRate() == null) fields.add("容量保持率");
        if (battery.getInternalResistanceRatio() == null) fields.add("内阻比");
        if (battery.getCycleCount() == null) fields.add("循环次数");
        if (battery.getAvgTemperature() == null) fields.add("平均温度");
        return fields;
    }

    private List<Map<String, Object>> generateTrendData(Double currentCapacityRetentionRate) {
        List<Map<String, Object>> trend = new ArrayList<Map<String, Object>>();
        double current = currentCapacityRetentionRate == null ? 80D : currentCapacityRetentionRate;
        double totalDrop = randomInRange(5D, 10D);
        double startValue = Math.min(100D, current + totalDrop);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        for (int i = 5; i >= 0; i--) {
            double retention = startValue - (5 - i) * (totalDrop / 5D);
            Map<String, Object> point = new LinkedHashMap<String, Object>();
            point.put("month", LocalDate.now().minusMonths(i).format(formatter));
            point.put("retention", scale(Math.max(current, retention)));
            trend.add(point);
        }
        trend.get(trend.size() - 1).put("retention", scale(current));
        return trend;
    }

    private HealthAssessmentVO toVO(HealthAssessment assessment, BatteryRecord batteryRecord) {
        HealthAssessmentVO vo = new HealthAssessmentVO();
        vo.setId(assessment.getId());
        vo.setBatteryId(assessment.getBatteryId());
        vo.setBatteryCode(batteryRecord == null ? null : batteryRecord.getBatteryCode());
        vo.setHealthScore(assessment.getHealthScore());
        vo.setHealthLevel(assessment.getHealthLevel());
        vo.setRuleScore(assessment.getRuleScore());
        vo.setMlScore(assessment.getMlScore());
        vo.setSuggestedScene(assessment.getSuggestedScene());
        vo.setLlmSummary(assessment.getLlmSummary());
        vo.setAssessmentTime(assessment.getAssessmentTime());
        vo.setIsMlEnhanced(assessment.getIsMlEnhanced());
        Report report = reportMapper.selectOne(new LambdaQueryWrapper<Report>()
                .eq(Report::getRelatedType, "ASSESSMENT")
                .eq(Report::getRelatedId, assessment.getId())
                .orderByDesc(Report::getId)
                .last("limit 1"));
        if (report != null) {
            vo.setReportId(report.getId());
            vo.setReportSummary(report.getSummary());
            vo.setReportContent(report.getContent());
        }
        vo.setBatteryRecord(batteryRecord);
        try {
            vo.setTrendData(objectMapper.readValue(assessment.getTrendData(), new TypeReference<List<Map<String, Object>>>() {}));
        } catch (Exception ex) {
            vo.setTrendData(new ArrayList<Map<String, Object>>());
        }
        return vo;
    }

    private BigDecimal scale(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private double randomInRange(double min, double max) {
        return min + (max - min) * ThreadLocalRandom.current().nextDouble();
    }

    private Double decimalValue(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    private BatteryRecord createBatteryFromDataset(Path csvFile, Long userId) throws Exception {
        List<String> lines = Files.readAllLines(csvFile).stream()
                .filter(line -> line != null && !line.trim().isEmpty())
                .limit(3)
                .collect(Collectors.toList());
        if (lines.size() < 2) {
            throw new IllegalArgumentException("CSV内容不足");
        }
        String[] headers = lines.get(0).split(",");
        String[] values = lines.get(1).split(",");
        double voltage = readValueByHeader(headers, values, 365D, "voltage", "电压", "v");
        double capacity = readValueByHeader(headers, values, 80D, "capacity", "soh", "容量");
        double resistance = readValueByHeader(headers, values, 1.2D, "internalresistance", "ir", "resistance", "内阻");
        int cycle = (int) Math.round(readValueByHeader(headers, values, 600D, "cycle", "cycles", "循环"));
        double temperature = readValueByHeader(headers, values, 28D, "temp", "temperature", "温度");

        BatteryRecord record = new BatteryRecord();
        record.setBatteryCode("EVB-DS-" + System.currentTimeMillis() + "-" + ThreadLocalRandom.current().nextInt(100, 999));
        record.setSourceType("dataset-import");
        record.setRemark(csvFile.getFileName().toString());
        record.setStatus("PENDING_ASSESSMENT");
        record.setAuditStatus(0);
        record.setCreatedBy(userId);
        record.setIsDeleted(Boolean.FALSE);
        record.setVoltage(scale(voltage));
        record.setCapacityRetentionRate(scale(capacity));
        record.setInternalResistanceRatio(scale(resistance));
        record.setCycleCount(cycle);
        record.setAvgTemperature(scale(temperature));
        Map<String, Object> features = new LinkedHashMap<String, Object>();
        features.put("voltage", record.getVoltage());
        features.put("capacityRetentionRate", record.getCapacityRetentionRate());
        features.put("internalResistanceRatio", record.getInternalResistanceRatio());
        features.put("cycleCount", record.getCycleCount());
        features.put("avgTemperature", record.getAvgTemperature());
        record.setFeatureJson(objectMapper.writeValueAsString(features));
        batteryRecordMapper.insert(record);
        return record;
    }

    private double readValueByHeader(String[] headers, String[] values, double defaultValue, String... aliases) {
        for (int i = 0; i < headers.length && i < values.length; i++) {
            String normalized = normalizeHeader(headers[i]);
            for (String alias : aliases) {
                if (normalized.contains(normalizeHeader(alias))) {
                    try {
                        return Double.parseDouble(values[i].trim());
                    } catch (Exception ignored) {
                        return defaultValue;
                    }
                }
            }
        }
        return defaultValue;
    }

    private String normalizeHeader(String value) {
        return value == null ? "" : value.toLowerCase().replace("_", "").replace("-", "").replace(" ", "");
    }

    private Long currentUserId() {
        Long userId = AuthUserContext.getCurrentUserId();
        return userId == null ? 1L : userId;
    }

    private void createAssessmentReport(HealthAssessment assessment, BatteryRecord battery) {
        Report report = new Report();
        report.setRelatedType("ASSESSMENT");
        report.setRelatedId(assessment.getId());
        report.setVersionNo("v1");
        report.setSummary("评估报告");
        report.setCreatedBy(battery == null ? null : battery.getCreatedBy());
        StringBuilder content = new StringBuilder();
        content.append("电池编码: ").append(battery == null ? "-" : battery.getBatteryCode()).append("；");
        content.append("健康评分: ").append(assessment.getHealthScore()).append("；");
        content.append("健康等级: ").append(assessment.getHealthLevel()).append("；");
        content.append("规则评分: ").append(assessment.getRuleScore()).append("；");
        content.append("机器学习评分: ").append(assessment.getMlScore() == null ? "未启用/不可用" : assessment.getMlScore()).append("；");
        content.append("建议场景: ").append(assessment.getSuggestedScene()).append("；");
        content.append("模型摘要: ").append(assessment.getLlmSummary());
        report.setContent(content.toString());
        reportMapper.insert(report);
    }

    private void pushAssessmentMessage(HealthAssessment assessment, BatteryRecord battery, String triggerMessage) {
        if (battery == null || battery.getCreatedBy() == null) {
            return;
        }
        String content = "电池[" + battery.getBatteryCode() + "]评估完成，健康分" + assessment.getHealthScore() + "，等级" +
                assessment.getHealthLevel() + "。说明: " + triggerMessage;
        jdbcTemplate.update(
                "insert into app_message(user_id, title, content, message_type, read_flag, related_type, related_id) values(?,?,?,?,?,?,?)",
                battery.getCreatedBy(), "评估完成通知", content, "ASSESSMENT", 0, "ASSESSMENT", assessment.getId()
        );
    }

    private Map<String, Object> buildTaskError(Long batteryId, String message) {
        Map<String, Object> error = new LinkedHashMap<String, Object>();
        error.put("batteryId", batteryId);
        error.put("message", message);
        return error;
    }

    private static class BatchAssessmentTask {
        private final int total;
        private final AtomicInteger completedCount = new AtomicInteger();
        private final AtomicInteger successCount = new AtomicInteger();
        private final AtomicInteger failCount = new AtomicInteger();
        private final List<HealthAssessmentVO> results = Collections.synchronizedList(new ArrayList<HealthAssessmentVO>());
        private final List<Map<String, Object>> errors = Collections.synchronizedList(new ArrayList<Map<String, Object>>());
        private volatile String status = "PENDING";
        private volatile boolean finished;
        private volatile String lastError;

        private BatchAssessmentTask(int total) {
            this.total = total;
        }

        public int getTotal() {
            return total;
        }

        public AtomicInteger getCompletedCount() {
            return completedCount;
        }

        public AtomicInteger getSuccessCount() {
            return successCount;
        }

        public AtomicInteger getFailCount() {
            return failCount;
        }

        public List<HealthAssessmentVO> getResults() {
            return results;
        }

        public List<Map<String, Object>> getErrors() {
            return errors;
        }

        public void setFailCount(int value) {
            failCount.set(Math.max(value, 0));
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public boolean isFinished() {
            return finished;
        }

        public void setFinished(boolean finished) {
            this.finished = finished;
        }

        public String getLastError() {
            return lastError;
        }

        public void setLastError(String lastError) {
            this.lastError = lastError;
        }
    }
}
