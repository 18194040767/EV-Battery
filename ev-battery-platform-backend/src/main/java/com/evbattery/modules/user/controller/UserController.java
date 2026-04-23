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
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

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
}
