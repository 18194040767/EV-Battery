package com.evbattery.modules.assistant.controller;

import com.evbattery.common.result.Result;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    @Value("${llm.enabled:true}")
    private boolean llmEnabled;

    @Value("${llm.api-url:}")
    private String llmApiUrl;

    @Value("${llm.model:glm-5.1}")
    private String llmModel;

    @Value("${llm.api-key:}")
    private String llmApiKey;

    @Resource
    private ObjectMapper objectMapper;

    @PostMapping("/chat")
    public Result<Object> chat(@RequestBody Map<String, Object> body) {
        String question = String.valueOf(body.getOrDefault("question", "")).trim();
        if (!StringUtils.hasText(question)) {
            return Result.fail(400, "请输入问题");
        }
        List<Map<String, Object>> history = normalizeHistory(body.get("history"));
        String reply = callGLM(question, history);
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("reply", reply);
        return Result.success(data);
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestBody Map<String, Object> body) {
        String question = String.valueOf(body.getOrDefault("question", "")).trim();
        List<Map<String, Object>> history = normalizeHistory(body.get("history"));
        SseEmitter emitter = new SseEmitter(0L);

        new Thread(() -> {
            try {
                if (!StringUtils.hasText(question)) {
                    emitter.send(SseEmitter.event().name("error").data("请输入问题"));
                    emitter.complete();
                    return;
                }

                String reply = callGLM(question, history);
                for (int i = 0; i < reply.length(); i++) {
                    emitter.send(SseEmitter.event().name("chunk").data(String.valueOf(reply.charAt(i))));
                    Thread.sleep(18L);
                }
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();
            } catch (Exception ex) {
                try {
                    emitter.send(SseEmitter.event().name("error").data("AI 小助手暂时不可用"));
                } catch (Exception ignored) {
                }
                emitter.completeWithError(ex);
            }
        }, "assistant-stream").start();

        return emitter;
    }

    private List<Map<String, Object>> normalizeHistory(Object source) {
        return source instanceof List
                ? objectMapper.convertValue(source, List.class)
                : new ArrayList<Map<String, Object>>();
    }

    private String callGLM(String question, List<Map<String, Object>> history) {
        if (llmEnabled && StringUtils.hasText(llmApiUrl) && StringUtils.hasText(llmApiKey)) {
            try {
                HttpURLConnection connection = openConnection();
                Map<String, Object> payload = buildPayload(question, history);
                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(objectMapper.writeValueAsBytes(payload));
                }
                try (InputStream inputStream = connection.getInputStream()) {
                    JsonNode root = objectMapper.readTree(inputStream);
                    String content = root.path("choices").path(0).path("message").path("content").asText("");
                    if (StringUtils.hasText(content)) {
                        return content.trim();
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return fallbackAnswer(question);
    }

    private HttpURLConnection openConnection() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(llmApiUrl).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + llmApiKey);
        return connection;
    }

    private Map<String, Object> buildPayload(String question, List<Map<String, Object>> history) {
        List<Map<String, Object>> messages = new ArrayList<Map<String, Object>>();
        messages.add(message("system",
                "你是电池梯次利用交易平台的 AI 助手。请使用简洁、专业、友好的中文回答。"
                        + "优先回答与电池档案、健康评估、商品交易、物流追踪、合同存证、支付模拟相关的问题。"
                        + "如果用户的问题超出平台范围，也可以提供简要建议，但不要编造平台不存在的功能。"));
        if (history != null) {
            for (Map<String, Object> item : history) {
                String role = String.valueOf(item.getOrDefault("role", "user"));
                String content = String.valueOf(item.getOrDefault("content", "")).trim();
                if (StringUtils.hasText(content) && ("user".equals(role) || "assistant".equals(role))) {
                    messages.add(message(role, content));
                }
            }
        }
        messages.add(message("user", question));

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("model", llmModel);
        payload.put("messages", messages);
        payload.put("temperature", 0.7D);
        return payload;
    }

    private Map<String, Object> message(String role, String content) {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("role", role);
        data.put("content", content);
        return data;
    }

    private String fallbackAnswer(String question) {
        String text = question.toLowerCase();
        if (text.contains("物流")) {
            return "可在物流追踪页输入运单号，查看地图轨迹、当前节点和预计到达时间。";
        }
        if (text.contains("合同")) {
            return "已完成订单可自动生成合同，并在合同中心进行在线预览、下载和查验。";
        }
        if (text.contains("评估")) {
            return "健康评估支持单体评估和批量评估，可查看评分、等级、建议场景和报告摘要。";
        }
        if (text.contains("下单") || text.contains("支付")) {
            return "商品下单后会进入待支付，选择收货地址后可完成模拟支付，并流转到待发货。";
        }
        return "我可以帮助你解答平台里的评估、商品、订单、物流和合同问题。你可以直接描述想做什么。";
    }
}
