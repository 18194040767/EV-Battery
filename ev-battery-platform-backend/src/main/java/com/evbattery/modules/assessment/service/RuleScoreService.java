package com.evbattery.modules.assessment.service;

public interface RuleScoreService {
    int calculateScore(Double capacityRetentionRate, Double internalResistanceRatio, Integer cycleCount, Double avgTemperature);

    String resolveHealthLevel(Integer score);

    String resolveSuggestedScene(String healthLevel);
}
