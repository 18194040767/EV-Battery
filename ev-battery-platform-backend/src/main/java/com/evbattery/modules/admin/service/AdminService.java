package com.evbattery.modules.admin.service;

import java.util.List;
import java.util.Map;

public interface AdminService {
    boolean isAdmin(Long userId);

    Map<String, Object> dashboard();

    List<Map<String, Object>> users();

    void updateUser(Map<String, Object> payload);

    void resetPassword(Long userId);

    List<Map<String, Object>> batteries();

    void auditBattery(Long batteryId, Integer auditStatus, String remark);

    List<Map<String, Object>> products();

    void auditProduct(Long productId, String auditStatus, String publishStatus);

    List<Map<String, Object>> orders();

    void forceCancelOrder(Long orderId);

    void markOrderShipped(Long orderId, String company, String trackingNo);

    List<Map<String, Object>> contracts();
}
