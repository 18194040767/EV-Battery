package com.evbattery.security;

import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {
    @Resource
    private JwtTokenUtil jwtTokenUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/user/login") || uri.startsWith("/api/user/register")
                || uri.startsWith("/api/assistant/")
                || uri.startsWith("/doc.html") || uri.startsWith("/v3/api-docs") || uri.startsWith("/swagger")) {
            return true;
        }
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            response.setStatus(401);
            return false;
        }
        try {
            String token = auth.substring(7);
            Claims claims = jwtTokenUtil.parse(token);
            request.setAttribute("userId", claims.get("userId"));
            request.setAttribute("username", claims.get("username"));
            return true;
        } catch (Exception e) {
            log.warn("JWT validate failed", e);
            response.setStatus(401);
            return false;
        }
    }
}
