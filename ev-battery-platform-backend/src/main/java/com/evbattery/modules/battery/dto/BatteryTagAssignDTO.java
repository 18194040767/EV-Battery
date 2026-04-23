package com.evbattery.modules.battery.dto;

import lombok.Data;

import java.util.List;

@Data
public class BatteryTagAssignDTO {
    private List<Long> tagIds;
    private List<String> tagNames;
}
