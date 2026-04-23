package com.evbattery.modules.assessment.vo;

import com.evbattery.modules.battery.entity.BatteryRecord;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class HealthAssessmentVO {
    private Long id;
    private Long batteryId;
    private String batteryCode;
    private Integer healthScore;
    private String healthLevel;
    private Integer ruleScore;
    private Integer mlScore;
    private String suggestedScene;
    private List<Map<String, Object>> trendData;
    private String llmSummary;
    private LocalDateTime assessmentTime;
    private Boolean isMlEnhanced;
    private Long reportId;
    private String reportSummary;
    private String reportContent;
    private BatteryRecord batteryRecord;
}
