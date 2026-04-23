package com.evbattery.modules.battery.dto;

import lombok.Data;

import java.util.List;

@Data
public class BatteryBatchDeleteDTO {
    private List<Long> ids;
}
