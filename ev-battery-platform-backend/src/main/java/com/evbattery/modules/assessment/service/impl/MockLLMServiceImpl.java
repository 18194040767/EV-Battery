package com.evbattery.modules.assessment.service.impl;

import com.evbattery.modules.assessment.service.LLMService;
import com.evbattery.modules.battery.entity.BatteryRecord;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class MockLLMServiceImpl implements LLMService {

    @Value("${llm.enabled:true}")
    private boolean llmEnabled;

    @Value("${llm.api-url:https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions}")
    private String llmApiUrl;

    @Value("${llm.model:qwen-plus}")
    private String llmModel;

    @Value("${llm.api-key:}")
    private String llmApiKey;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public String generateSummary(Integer score, String healthLevel, String suggestedScene, BatteryRecord batteryRecord) {
        if (llmEnabled && StringUtils.hasText(llmApiKey)) {
            String result = callRemoteSummary(score, healthLevel, suggestedScene, batteryRecord);
            if (StringUtils.hasText(result)) {
                return result;
            }
        }
        String remainingLife = resolveRemainingLife(healthLevel);
        return String.format(
                "该电池健康评分%d分，等级%s。容量保持率%s%%，内阻增加比例%s，循环次数%d次，平均温度%s℃。建议用于%s。预计剩余寿命约%s。当前为本地生成的评估摘要。",
                score,
                healthLevel,
                decimalText(batteryRecord.getCapacityRetentionRate()),
                decimalText(batteryRecord.getInternalResistanceRatio()),
                batteryRecord.getCycleCount() == null ? 0 : batteryRecord.getCycleCount(),
                decimalText(batteryRecord.getAvgTemperature()),
                suggestedScene,
                remainingLife
        );
    }

    private String callRemoteSummary(Integer score, String healthLevel, String suggestedScene, BatteryRecord batteryRecord) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(llmApiKey);

            String prompt = String.format(
                    "请基于以下电池评估结果生成一句简短中文建议（不超过80字）：评分%s，等级%s，容量保持率%s，内阻比%s，循环次数%s，平均温度%s，建议场景%s。",
                    score, healthLevel, decimalText(batteryRecord.getCapacityRetentionRate()),
                    decimalText(batteryRecord.getInternalResistanceRatio()), batteryRecord.getCycleCount(),
                    decimalText(batteryRecord.getAvgTemperature()), suggestedScene
            );

            Map<String, Object> userMessage = new LinkedHashMap<String, Object>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            Map<String, Object> body = new LinkedHashMap<String, Object>();
            body.put("model", llmModel);
            body.put("messages", Arrays.asList(userMessage));
            body.put("temperature", 0.3D);

            ResponseEntity<String> response = restTemplate.postForEntity(llmApiUrl, new HttpEntity<Map<String, Object>>(body, headers), String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").path(0).path("message").path("content").asText("");
        } catch (Exception ex) {
            return null;
        }
    }

    private String resolveRemainingLife(String healthLevel) {
        if ("优秀".equals(healthLevel)) {
            return "3-5年";
        }
        if ("良好".equals(healthLevel)) {
            return "2-3年";
        }
        if ("一般".equals(healthLevel)) {
            return "1-2年";
        }
        if ("较差".equals(healthLevel)) {
            return "1年以内";
        }
        return "建议停止使用";
    }

    private String decimalText(Object value) {
        return value == null ? "0" : String.valueOf(value);
    }
}
