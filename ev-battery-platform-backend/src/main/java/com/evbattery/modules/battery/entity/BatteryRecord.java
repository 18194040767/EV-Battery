package com.evbattery.modules.battery.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.evbattery.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("battery_record")
public class BatteryRecord extends BaseEntity {
    private Long id;
    private String batteryCode;
    private String sourceType;
    private String bmsRawFilePath;
    private String featureJson;
    private Integer auditStatus;
    private String status;
    private String remark;
    private Boolean isDeleted;
    private Long createdBy;
    private BigDecimal voltage;
    private BigDecimal capacityRetentionRate;
    private BigDecimal internalResistanceRatio;
    private Integer cycleCount;
    private BigDecimal avgTemperature;
}
