package com.evbattery.modules.logistics.service;

import java.util.Map;

public interface LogisticsService {
    Map<String, Object> saveTracking(Long orderId, String company, String trackingNo, String contactName, String contactPhone, Long currentUserId, boolean admin);

    Map<String, Object> queryTracking(Long orderId, Long currentUserId, boolean admin);

    byte[] loadHazardousNoticePdf(Long orderId, Long currentUserId, boolean admin);
}
