package com.evbattery.modules.battery.dto;

import lombok.Data;

import java.util.Map;

@Data
public class BatteryDraftDTO {
    private Long id;
    private String title;
    private Map<String, Object> draftData;
}
