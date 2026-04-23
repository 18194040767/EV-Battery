package com.evbattery.modules.assessment;

import com.evbattery.EvBatteryPlatformApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = EvBatteryPlatformApplication.class,
        properties = {
                "llm.enabled=false",
                "spring.main.allow-bean-definition-overriding=true"
        }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AssessmentApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void shouldKeepLongIdsAsStringsDuringAssessmentFlow() throws Exception {
        String token = loginAndGetToken();
        String batteryId = createManualBattery(token, "ID precision smoke test");

        assertTrue(batteryId.length() > 15, "battery id should exceed JS safe integer length");

        JsonNode triggerData = readData(
                mockMvc.perform(post("/api/assessment/trigger")
                                .header("Authorization", "Bearer " + token)
                                .param("batteryId", batteryId)
                                .param("useML", "false"))
                        .andExpect(status().isOk())
                        .andReturn()
        );

        assertEquals(batteryId, triggerData.path("batteryId").asText());
        assertFalse(triggerData.path("reportContent").asText().isEmpty());
        assertFalse(triggerData.path("llmSummary").asText().isEmpty());

        JsonNode latestData = readData(
                mockMvc.perform(get("/api/assessment/battery/{batteryId}/latest", batteryId)
                                .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn()
        );

        assertEquals(batteryId, latestData.path("batteryId").asText());
        assertEquals(batteryId, latestData.path("batteryRecord").path("id").asText());
    }

    @Test
    public void shouldReturnBatchResultsForFrontendRendering() throws Exception {
        String token = loginAndGetToken();
        List<String> batteryIds = new ArrayList<String>();
        batteryIds.add(createManualBattery(token, "batch test A"));
        batteryIds.add(createManualBattery(token, "batch test B"));

        String payload = String.format("{\"batteryIds\":[\"%s\",\"%s\"],\"useML\":false}", batteryIds.get(0), batteryIds.get(1));
        JsonNode taskData = readData(
                mockMvc.perform(post("/api/assessment/batch/trigger")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                        .andExpect(status().isOk())
                        .andReturn()
        );

        String taskId = taskData.path("taskId").asText();
        assertFalse(taskId.isEmpty());

        JsonNode statusData = null;
        for (int i = 0; i < 40; i++) {
            statusData = readData(
                    mockMvc.perform(get("/api/assessment/batch/task/{taskId}", taskId)
                                    .header("Authorization", "Bearer " + token))
                            .andExpect(status().isOk())
                            .andReturn()
            );
            if (statusData.path("finished").asBoolean()) {
                break;
            }
            Thread.sleep(200L);
        }

        assertNotNull(statusData);
        assertTrue(statusData.path("finished").asBoolean(), "batch task should finish in test window");
        assertEquals(2, statusData.path("results").size());
        assertEquals(0, statusData.path("failCount").asInt());
    }

    private String loginAndGetToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"seller01\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return readData(result).path("token").asText();
    }

    private String createManualBattery(String token, String remark) throws Exception {
        String payload = String.format(
                "{\"sourceType\":\"manual-test\",\"status\":\"PENDING_ASSESSMENT\",\"voltage\":365,\"capacityRetentionRate\":88,\"internalResistanceRatio\":0.18,\"cycleCount\":420,\"avgTemperature\":26,\"remark\":\"%s\"}",
                remark
        );
        JsonNode data = readData(
                mockMvc.perform(post("/api/battery/manual")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                        .andExpect(status().isOk())
                        .andReturn()
        );
        return data.path("id").asText();
    }

    private JsonNode readData(MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data");
    }
}
