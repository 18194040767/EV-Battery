package com.evbattery.modules.assessment.service;

import com.evbattery.modules.battery.entity.BatteryRecord;

public interface LLMService {
    String generateSummary(Integer score, String healthLevel, String suggestedScene, BatteryRecord batteryRecord);
}
