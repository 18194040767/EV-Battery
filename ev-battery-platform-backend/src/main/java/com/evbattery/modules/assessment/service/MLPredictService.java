package com.evbattery.modules.assessment.service;

import com.evbattery.modules.battery.entity.BatteryRecord;

public interface MLPredictService {
    Integer predictSoh(BatteryRecord batteryRecord);
}
