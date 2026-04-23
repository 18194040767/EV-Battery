package com.evbattery.modules.assessment.service.impl;

import com.evbattery.modules.assessment.service.RuleScoreService;
import org.springframework.stereotype.Service;

@Service
public class RuleScoreServiceImpl implements RuleScoreService {

    @Override
    public int calculateScore(Double capacityRetentionRate, Double internalResistanceRatio, Integer cycleCount, Double avgTemperature) {
        double capScore = clamp(capacityRetentionRate, 0D, 100D);
        double irScore = Math.max(0D, (1D - defaultValue(internalResistanceRatio, 0D)) * 100D);
        double cycleScore = resolveCycleScore(cycleCount);
        double tempScore = resolveTemperatureScore(avgTemperature);
        double weighted = capScore * 0.50 + irScore * 0.25 + cycleScore * 0.20 + tempScore * 0.05;
        return (int) Math.round(weighted);
    }

    @Override
    public String resolveHealthLevel(Integer score) {
        int value = score == null ? 0 : score;
        if (value >= 90) {
            return "优秀";
        }
        if (value >= 75) {
            return "良好";
        }
        if (value >= 60) {
            return "一般";
        }
        if (value >= 40) {
            return "较差";
        }
        return "淘汰";
    }

    @Override
    public String resolveSuggestedScene(String healthLevel) {
        if ("优秀".equals(healthLevel) || "良好".equals(healthLevel)) {
            return "低速电动车/家庭储能";
        }
        if ("一般".equals(healthLevel)) {
            return "备用电源";
        }
        if ("较差".equals(healthLevel)) {
            return "低功率设备/回收梯次利用";
        }
        return "报废处理";
    }

    private double resolveCycleScore(Integer cycleCount) {
        int cycles = cycleCount == null ? 0 : cycleCount;
        if (cycles < 500) {
            return 100D;
        }
        if (cycles <= 1000) {
            return 80D;
        }
        if (cycles <= 2000) {
            return 50D;
        }
        return 20D;
    }

    private double resolveTemperatureScore(Double avgTemperature) {
        double temperature = defaultValue(avgTemperature, 25D);
        if (temperature < 15D) {
            return 70D;
        }
        if (temperature <= 35D) {
            return 100D;
        }
        if (temperature <= 45D) {
            return 60D;
        }
        return 30D;
    }

    private double clamp(Double value, double min, double max) {
        return Math.max(min, Math.min(max, defaultValue(value, min)));
    }

    private double defaultValue(Double value, double defaultValue) {
        return value == null ? defaultValue : value;
    }
}
