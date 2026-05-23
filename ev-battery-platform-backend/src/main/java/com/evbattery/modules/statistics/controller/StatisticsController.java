package com.evbattery.modules.statistics.controller;

import com.evbattery.common.result.Result;
import com.evbattery.modules.statistics.service.StatisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    @Resource
    private StatisticsService statisticsService;

    @GetMapping("/trade-trend")
    public Result<Object> tradeTrend(@RequestParam(required = false) Integer days) {
        return Result.success(statisticsService.tradeTrend(days));
    }

    @GetMapping("/health-distribution")
    public Result<Object> healthDistribution(@RequestParam(required = false) Integer days) {
        return Result.success(statisticsService.healthDistribution(days));
    }

    @GetMapping("/source-distribution")
    public Result<Object> sourceDistribution() {
        return Result.success(statisticsService.sourceDistribution());
    }

    @GetMapping("/product-category-distribution")
    public Result<Object> productCategoryDistribution(@RequestParam(required = false) Integer days) {
        return Result.success(statisticsService.productCategoryDistribution(days));
    }
}
