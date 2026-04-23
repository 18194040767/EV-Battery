package com.evbattery.modules.battery.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.evbattery.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("battery_draft")
public class BatteryDraft extends BaseEntity {
    private Long id;
    private Long userId;
    private String title;
    private String draftData;
}
