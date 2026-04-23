package com.evbattery.modules.assessment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.evbattery.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("health_assessment")
public class HealthAssessment extends BaseEntity {
    private Long id;
    private Long batteryId;
    private Integer healthScore;
    private String healthLevel;
    private Integer ruleScore;
    private Integer mlScore;
    private String suggestedScene;
    private String trendData;
    private String llmSummary;
    private LocalDateTime assessmentTime;
    private Boolean isMlEnhanced;

    public Double getScore() {
        return healthScore == null ? null : healthScore.doubleValue();
    }

    public String getGrade() {
        return healthLevel;
    }

    public String getSuggestion() {
        return suggestedScene;
    }

    public String getVisualizationJson() {
        return trendData;
    }
}
