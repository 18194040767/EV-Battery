package com.evbattery.modules.report.service.impl;
import com.evbattery.common.result.Result;
import com.evbattery.modules.report.service.ReportService;
import org.springframework.stereotype.Service;
@Service
public class ReportServiceImpl implements ReportService { @Override public Result<?> placeholder() { return Result.success("report service placeholder", null); } }
