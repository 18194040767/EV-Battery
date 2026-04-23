package com.evbattery.modules.statistics.service;

import java.util.List;
import java.util.Map;

public interface StatisticsService {
    Map<String, Object> tradeTrend(Integer days);

    List<Map<String, Object>> healthDistribution();

    List<Map<String, Object>> sourceDistribution();

    List<Map<String, Object>> productCategoryDistribution();

    Map<String, Object> adminOverview();
}
