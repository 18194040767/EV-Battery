package com.evbattery.modules.trade.service.impl;
import com.evbattery.common.result.Result;
import com.evbattery.modules.trade.service.TradeService;
import org.springframework.stereotype.Service;
@Service
public class TradeServiceImpl implements TradeService { @Override public Result<?> placeholder() { return Result.success("trade service placeholder", null); } }
