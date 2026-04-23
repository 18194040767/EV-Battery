package com.evbattery.modules.assessment.service.impl;
import com.evbattery.common.result.Result;
import com.evbattery.modules.assessment.service.AssessmentService;
import org.springframework.stereotype.Service;
@Service
public class AssessmentServiceImpl implements AssessmentService { @Override public Result<?> placeholder() { return Result.success("assessment service placeholder", null); } }
