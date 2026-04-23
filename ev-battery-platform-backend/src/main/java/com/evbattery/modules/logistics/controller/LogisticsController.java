package com.evbattery.modules.logistics.controller;

import com.evbattery.common.AuthUserContext;
import com.evbattery.common.result.Result;
import com.evbattery.modules.admin.service.AdminService;
import com.evbattery.modules.logistics.service.LogisticsService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;

@RestController
@RequestMapping("/api/logistics")
public class LogisticsController {

    @Resource
    private LogisticsService logisticsService;

    @Resource
    private AdminService adminService;

    @PostMapping("/fill-tracking")
    public Result<Object> fill(@RequestBody Map<String, Object> payload) {
        Long currentUserId = AuthUserContext.getCurrentUserId();
        return Result.success(logisticsService.saveTracking(
                Long.parseLong(String.valueOf(payload.get("orderId"))),
                payload.get("company") == null ? null : String.valueOf(payload.get("company")),
                payload.get("trackingNo") == null ? null : String.valueOf(payload.get("trackingNo")),
                payload.get("contactName") == null ? null : String.valueOf(payload.get("contactName")),
                payload.get("contactPhone") == null ? null : String.valueOf(payload.get("contactPhone")),
                currentUserId,
                adminService.isAdmin(currentUserId)
        ));
    }

    @GetMapping("/status/{orderId}")
    public Result<Object> status(@PathVariable Long orderId) {
        Long currentUserId = AuthUserContext.getCurrentUserId();
        return Result.success(logisticsService.queryTracking(orderId, currentUserId, adminService.isAdmin(currentUserId)));
    }

    @GetMapping("/{orderId}/hazardous-notice")
    public ResponseEntity<byte[]> notice(@PathVariable Long orderId) {
        Long currentUserId = AuthUserContext.getCurrentUserId();
        byte[] bytes = logisticsService.loadHazardousNoticePdf(orderId, currentUserId, adminService.isAdmin(currentUserId));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=hazardous-notice-" + orderId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(bytes.length)
                .body(bytes);
    }
}
