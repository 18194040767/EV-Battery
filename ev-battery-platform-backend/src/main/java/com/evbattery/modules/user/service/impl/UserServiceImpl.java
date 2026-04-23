package com.evbattery.modules.user.service.impl;
import com.evbattery.common.result.Result;
import com.evbattery.modules.user.service.UserService;
import org.springframework.stereotype.Service;
@Service
public class UserServiceImpl implements UserService { @Override public Result<?> placeholder() { return Result.success("user service placeholder", null); } }
