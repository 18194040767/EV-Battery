package com.evbattery.modules.admin.controller;

import com.evbattery.common.AuthUserContext;
import com.evbattery.common.result.Result;
import com.evbattery.modules.admin.service.AdminService;
import com.evbattery.modules.contract.service.ContractService;
import com.evbattery.modules.logistics.service.LogisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Resource
    private AdminService adminService;

    @Resource
    private ContractService contractService;

    @Resource
    private LogisticsService logisticsService;

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        if (!adminService.isAdmin(AuthUserContext.getCurrentUserId())) {
            return Result.fail(403, "仅管理员可访问后台管理");
        }
        return Result.success(adminService.dashboard());
    }

    @GetMapping("/users")
    public Result<Object> users() {
        if (!adminService.isAdmin(AuthUserContext.getCurrentUserId())) {
            return Result.fail(403, "仅管理员可访问后台管理");
        }
        return Result.success(adminService.users());
    }

    @PutMapping("/users")
    public Result<String> updateUser(@RequestBody Map<String, Object> payload) {
        if (!adminService.isAdmin(AuthUserContext.getCurrentUserId())) {
            return Result.fail(403, "仅管理员可访问后台管理");
        }
        adminService.updateUser(payload);
        return Result.success("用户信息已更新", null);
    }

    @PostMapping("/users/{id}/reset-password")
    public Result<String> resetPassword(@PathVariable Long id) {
        if (!adminService.isAdmin(AuthUserContext.getCurrentUserId())) {
            return Result.fail(403, "仅管理员可访问后台管理");
        }
        adminService.resetPassword(id);
        return Result.success("密码已重置为 123456", null);
    }

    @GetMapping("/batteries")
    public Result<Object> batteries() {
        if (!adminService.isAdmin(AuthUserContext.getCurrentUserId())) {
            return Result.fail(403, "仅管理员可访问后台管理");
        }
        return Result.success(adminService.batteries());
    }

    @PostMapping("/batteries/{id}/audit")
    public Result<String> auditBattery(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        if (!adminService.isAdmin(AuthUserContext.getCurrentUserId())) {
            return Result.fail(403, "仅管理员可访问后台管理");
        }
        adminService.auditBattery(id, Integer.parseInt(String.valueOf(payload.getOrDefault("auditStatus", 1))), String.valueOf(payload.getOrDefault("remark", "")));
        return Result.success("电池档案审核结果已更新", null);
    }

    @GetMapping("/products")
    public Result<Object> products() {
        if (!adminService.isAdmin(AuthUserContext.getCurrentUserId())) {
            return Result.fail(403, "仅管理员可访问后台管理");
        }
        return Result.success(adminService.products());
    }

    @PostMapping("/products/{id}/audit")
    public Result<String> auditProduct(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        if (!adminService.isAdmin(AuthUserContext.getCurrentUserId())) {
            return Result.fail(403, "仅管理员可访问后台管理");
        }
        adminService.auditProduct(id, String.valueOf(payload.getOrDefault("auditStatus", "APPROVED")), String.valueOf(payload.getOrDefault("publishStatus", "ON_SHELF")));
        return Result.success("商品状态已更新", null);
    }

    @GetMapping("/orders")
    public Result<Object> orders() {
        if (!adminService.isAdmin(AuthUserContext.getCurrentUserId())) {
            return Result.fail(403, "仅管理员可访问后台管理");
        }
        return Result.success(adminService.orders());
    }

    @PostMapping("/orders/{id}/cancel")
    public Result<String> forceCancel(@PathVariable Long id) {
        if (!adminService.isAdmin(AuthUserContext.getCurrentUserId())) {
            return Result.fail(403, "仅管理员可访问后台管理");
        }
        adminService.forceCancelOrder(id);
        return Result.success("订单已取消", null);
    }

    @PostMapping("/orders/{id}/ship")
    public Result<Object> ship(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> payload) {
        if (!adminService.isAdmin(AuthUserContext.getCurrentUserId())) {
            return Result.fail(403, "仅管理员可访问后台管理");
        }
        adminService.markOrderShipped(id, payload == null ? null : String.valueOf(payload.get("company")), payload == null ? null : String.valueOf(payload.get("trackingNo")));
        return Result.success(logisticsService.saveTracking(
                id,
                payload == null ? null : String.valueOf(payload.get("company")),
                payload == null ? null : String.valueOf(payload.get("trackingNo")),
                payload == null ? null : String.valueOf(payload.get("contactName")),
                payload == null ? null : String.valueOf(payload.get("contactPhone")),
                AuthUserContext.getCurrentUserId(),
                true
        ));
    }

    @GetMapping("/contracts")
    public Result<Object> contracts() {
        if (!adminService.isAdmin(AuthUserContext.getCurrentUserId())) {
            return Result.fail(403, "仅管理员可访问后台管理");
        }
        return Result.success(adminService.contracts());
    }

    @PostMapping("/contracts/{id}/verify")
    public Result<Object> verifyContract(@PathVariable Long id) {
        if (!adminService.isAdmin(AuthUserContext.getCurrentUserId())) {
            return Result.fail(403, "仅管理员可访问后台管理");
        }
        return Result.success(contractService.verifyContractById(id));
    }
}
