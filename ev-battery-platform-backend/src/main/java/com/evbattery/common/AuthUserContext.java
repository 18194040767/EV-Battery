package com.evbattery.common;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

public class AuthUserContext {
    private AuthUserContext() {
    }

    public static Long getCurrentUserId() {
        Object val = getAttribute("userId");
        if (val == null) {
            return null;
        }
        if (val instanceof Integer) {
            return ((Integer) val).longValue();
        }
        if (val instanceof Long) {
            return (Long) val;
        }
        return Long.parseLong(String.valueOf(val));
    }

    public static String getCurrentUsername() {
        Object val = getAttribute("username");
        return val == null ? null : String.valueOf(val);
    }

    private static Object getAttribute(String key) {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (!(attrs instanceof ServletRequestAttributes)) {
            return null;
        }
        HttpServletRequest request = ((ServletRequestAttributes) attrs).getRequest();
        return request.getAttribute(key);
    }
}

