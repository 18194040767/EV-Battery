package com.evbattery.modules.battery.service.impl;
import com.evbattery.common.result.Result;
import com.evbattery.modules.battery.service.BatteryService;
import org.springframework.stereotype.Service;
@Service
public class BatteryServiceImpl implements BatteryService { @Override public Result<?> placeholder() { return Result.success("battery service placeholder", null); } }
