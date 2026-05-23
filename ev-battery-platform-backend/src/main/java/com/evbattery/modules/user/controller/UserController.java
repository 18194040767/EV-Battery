package com.evbattery.modules.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.evbattery.common.AuthUserContext;
import com.evbattery.common.result.Result;
import com.evbattery.modules.user.entity.User;
import com.evbattery.modules.user.mapper.UserMapper;
import com.evbattery.security.JwtTokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private static final String RESET_PASSWORD_PREFIX = "auth:reset-password:";
    private static final long RESET_CODE_TTL_SECONDS = 10 * 60;
    private static final long RESET_CODE_RESEND_SECONDS = 60;
    private final Map<String, ResetCodeRecord> localResetCodes = new ConcurrentHashMap<String, ResetCodeRecord>();

    @Resource
    private JwtTokenUtil jwtTokenUtil;

    @Resource
    private UserMapper userMapper;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> dto) {
        String username = dto.getOrDefault("username", "").trim();
        String password = dto.getOrDefault("password", "");
        if (username.isEmpty() || password.isEmpty()) {
            return Result.fail(400, "用户名或密码为空");
        }

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null || user.getStatus() == 0 || !passwordEncoder.matches(password, user.getPassword())) {
            return Result.fail(401, "用户名或密码错误");
        }

        String token = jwtTokenUtil.generateToken(user.getId(), user.getUsername());
        List<String> roles = jdbcTemplate.query(
                "select r.role_code from role r join user_role ur on r.id=ur.role_id where ur.user_id=?",
                (rs, rowNum) -> rs.getString(1),
                user.getId()
        );
        safeWriteRedis("auth:last-login:" + user.getId(), user.getUsername(), 30, TimeUnit.DAYS);

        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("roles", roles);
        return Result.success("登录成功", data);
    }

    @PostMapping("/register")
    public Result<String> register(@RequestBody Map<String, String> dto) {
        String username = dto.getOrDefault("username", "").trim();
        String password = dto.getOrDefault("password", "");
        String email = dto.getOrDefault("email", "").trim();
        if (username.isEmpty() || password.isEmpty()) {
            return Result.fail(400, "用户名或密码为空");
        }

        Integer count = jdbcTemplate.queryForObject("select count(1) from `user` where username=?", Integer.class, username);
        if (count != null && count > 0) {
            return Result.fail(400, "用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setRealName(dto.getOrDefault("realName", username));
        user.setStatus(1);
        userMapper.insert(user);

        Long roleUserId = jdbcTemplate.queryForObject("select id from role where role_code='ROLE_USER'", Long.class);
        jdbcTemplate.update("insert into user_role(user_id, role_id) values(?,?)", user.getId(), roleUserId);
        safeWriteRedis("auth:registered:" + username, email.isEmpty() ? "-" : email, 7, TimeUnit.DAYS);
        return Result.success("注册成功", null);
    }

    @GetMapping("/current")
    public Result<Map<String, Object>> current() {
        Long userId = AuthUserContext.getCurrentUserId();
        if (userId == null) {
            return Result.fail(401, "未登录");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.fail(404, "用户不存在");
        }

        List<String> roles = jdbcTemplate.query(
                "select r.role_code from role r join user_role ur on r.id=ur.role_id where ur.user_id=?",
                (rs, rowNum) -> rs.getString(1),
                userId
        );
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        data.put("email", user.getEmail());
        data.put("roles", roles);
        return Result.success(data);
    }

    @PostMapping("/change-password")
    public Result<String> changePassword(@RequestBody Map<String, String> dto) {
        Long userId = AuthUserContext.getCurrentUserId();
        if (userId == null) {
            return Result.fail(401, "未登录");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.fail(404, "用户不存在");
        }
        if (!passwordEncoder.matches(dto.getOrDefault("oldPassword", ""), user.getPassword())) {
            return Result.fail(400, "原密码错误");
        }

        user.setPassword(passwordEncoder.encode(dto.getOrDefault("newPassword", "")));
        userMapper.updateById(user);
        safeDeleteRedis("auth:last-login:" + userId);
        return Result.success("修改成功", null);
    }

    @PostMapping("/forgot-password/code")
    public Result<Map<String, Object>> requestForgotPasswordCode(@RequestBody Map<String, String> dto) {
        String username = dto.getOrDefault("username", "").trim();
        String email = dto.getOrDefault("email", "").trim();
        if (username.isEmpty() || email.isEmpty()) {
            return Result.fail(400, "请输入账号和注册邮箱");
        }

        User user = findEnabledUserByUsername(username);
        if (user == null) {
            return Result.fail(404, "账号不存在或已停用");
        }
        if (user.getEmail() == null || !user.getEmail().trim().equalsIgnoreCase(email)) {
            return Result.fail(400, "注册邮箱与账号不匹配");
        }

        String key = resetPasswordKey(username);
        long now = System.currentTimeMillis();
        ResetCodeRecord current = readResetCode(key);
        if (current != null && !current.isExpired(now)
                && now - current.getCreatedAtMillis() < RESET_CODE_RESEND_SECONDS * 1000) {
            return Result.fail(429, "验证码获取过于频繁，请稍后再试");
        }

        String code = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
        ResetCodeRecord record = new ResetCodeRecord(code, now + RESET_CODE_TTL_SECONDS * 1000, now);
        storeResetCode(key, record);

        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("code", code);
        data.put("expiresInSeconds", RESET_CODE_TTL_SECONDS);
        data.put("message", "模拟验证码已生成，请在 10 分钟内使用");
        return Result.success("验证码已生成", data);
    }

    @PostMapping("/forgot-password/reset")
    public Result<String> resetForgotPassword(@RequestBody Map<String, String> dto) {
        String username = dto.getOrDefault("username", "").trim();
        String email = dto.getOrDefault("email", "").trim();
        String code = dto.getOrDefault("code", "").trim();
        String newPassword = dto.getOrDefault("newPassword", "");
        if (username.isEmpty() || email.isEmpty() || code.isEmpty() || newPassword.isEmpty()) {
            return Result.fail(400, "账号、邮箱、验证码和新密码不能为空");
        }
        if (newPassword.length() < 6) {
            return Result.fail(400, "新密码至少 6 位");
        }

        User user = findEnabledUserByUsername(username);
        if (user == null) {
            return Result.fail(404, "账号不存在或已停用");
        }
        if (user.getEmail() == null || !user.getEmail().trim().equalsIgnoreCase(email)) {
            return Result.fail(400, "注册邮箱与账号不匹配");
        }

        String key = resetPasswordKey(username);
        long now = System.currentTimeMillis();
        ResetCodeRecord record = readResetCode(key);
        if (record == null || record.isExpired(now)) {
            deleteResetCode(key);
            return Result.fail(400, "验证码已过期或不存在，请重新获取");
        }
        if (!record.getCode().equals(code)) {
            return Result.fail(400, "验证码错误");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        deleteResetCode(key);
        safeDeleteRedis("auth:last-login:" + user.getId());
        return Result.success("密码已重置，请使用新密码登录", null);
    }

    private void safeWriteRedis(String key, String value, long timeout, TimeUnit unit) {
        try {
            stringRedisTemplate.opsForValue().set(key, value, timeout, unit);
        } catch (DataAccessException ex) {
            log.warn("Skip redis write for key {}", key, ex);
        }
    }

    private void safeDeleteRedis(String key) {
        try {
            stringRedisTemplate.delete(key);
        } catch (DataAccessException ex) {
            log.warn("Skip redis delete for key {}", key, ex);
        }
    }

    private User findEnabledUserByUsername(String username) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null || user.getStatus() == 0) {
            return null;
        }
        return user;
    }

    private String resetPasswordKey(String username) {
        return RESET_PASSWORD_PREFIX + username;
    }

    private void storeResetCode(String key, ResetCodeRecord record) {
        localResetCodes.put(key, record);
        try {
            stringRedisTemplate.opsForValue().set(key, record.serialize(), RESET_CODE_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (DataAccessException ex) {
            log.warn("Fallback to local reset code store for key {}", key, ex);
        }
    }

    private ResetCodeRecord readResetCode(String key) {
        try {
            String value = stringRedisTemplate.opsForValue().get(key);
            ResetCodeRecord record = ResetCodeRecord.parse(value);
            if (record != null) {
                return record;
            }
        } catch (DataAccessException ex) {
            log.warn("Read reset code from redis failed for key {}", key, ex);
        }
        ResetCodeRecord localRecord = localResetCodes.get(key);
        if (localRecord != null && localRecord.isExpired(System.currentTimeMillis())) {
            localResetCodes.remove(key);
            return null;
        }
        return localRecord;
    }

    private void deleteResetCode(String key) {
        localResetCodes.remove(key);
        safeDeleteRedis(key);
    }

    private static class ResetCodeRecord {
        private final String code;
        private final long expireAtMillis;
        private final long createdAtMillis;

        private ResetCodeRecord(String code, long expireAtMillis, long createdAtMillis) {
            this.code = code;
            this.expireAtMillis = expireAtMillis;
            this.createdAtMillis = createdAtMillis;
        }

        private String getCode() {
            return code;
        }

        private long getCreatedAtMillis() {
            return createdAtMillis;
        }

        private boolean isExpired(long now) {
            return now > expireAtMillis;
        }

        private String serialize() {
            return code + "|" + expireAtMillis + "|" + createdAtMillis;
        }

        private static ResetCodeRecord parse(String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            String[] parts = value.split("\\|");
            if (parts.length != 3) {
                return null;
            }
            try {
                return new ResetCodeRecord(parts[0], Long.parseLong(parts[1]), Long.parseLong(parts[2]));
            } catch (NumberFormatException ex) {
                return null;
            }
        }
    }
}
