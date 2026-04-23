package com.evbattery.modules.message.controller;

import com.evbattery.common.AuthUserContext;
import com.evbattery.common.result.Result;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/message")
public class MessageController {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/list")
    public Result<Map<String, Object>> list(@RequestParam(defaultValue = "1") Integer page,
                                            @RequestParam(defaultValue = "10") Integer size) {
        Long userId = currentUserId();
        int currentPage = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : Math.min(size, 100);
        Integer total = jdbcTemplate.queryForObject("select count(1) from app_message where user_id = ?", Integer.class, userId);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select id, title, content, message_type as messageType, read_flag as readFlag, related_type as relatedType, related_id as relatedId, created_at as createdAt " +
                        "from app_message where user_id = ? order by id desc limit ?, ?",
                userId, (currentPage - 1) * pageSize, pageSize
        );
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("records", rows);
        data.put("total", total == null ? 0 : total);
        data.put("page", currentPage);
        data.put("size", pageSize);
        return Result.success(data);
    }

    @GetMapping("/unread-count")
    public Result<Map<String, Object>> unreadCount() {
        Long userId = currentUserId();
        Integer count = jdbcTemplate.queryForObject("select count(1) from app_message where user_id = ? and read_flag = 0", Integer.class, userId);
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("unreadCount", count == null ? 0 : count);
        return Result.success(data);
    }

    @PostMapping("/{id}/read")
    public Result<String> markRead(@PathVariable Long id) {
        jdbcTemplate.update("update app_message set read_flag = 1 where id = ? and user_id = ?", id, currentUserId());
        return Result.success("已读", null);
    }

    @PostMapping("/read-all")
    public Result<String> markAllRead() {
        jdbcTemplate.update("update app_message set read_flag = 1 where user_id = ? and read_flag = 0", currentUserId());
        return Result.success("全部已读", null);
    }

    private Long currentUserId() {
        Long userId = AuthUserContext.getCurrentUserId();
        return userId == null ? 1L : userId;
    }
}
