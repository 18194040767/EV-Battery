package com.evbattery.modules.battery.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class BatteryManualDTO {
    private String sourceType;
    private String remark;
    private BigDecimal voltage;
    private BigDecimal capacityRetentionRate;
    private BigDecimal internalResistanceRatio;
    private Integer cycleCount;
    private BigDecimal avgTemperature;
    private String status;
    private Long draftId;
    private List<Long> tagIds;
    private List<String> tagNames;
    private Map<String, Object> extraFeatures;
}
