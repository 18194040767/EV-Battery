package com.evbattery.modules.report.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.evbattery.common.AuthUserContext;
import com.evbattery.common.result.Result;
import com.evbattery.modules.assessment.entity.HealthAssessment;
import com.evbattery.modules.assessment.mapper.HealthAssessmentMapper;
import com.evbattery.modules.report.entity.Report;
import com.evbattery.modules.report.mapper.ReportMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/report")
public class ReportController {
    @Resource
    private ReportMapper reportMapper;
    @Resource
    private HealthAssessmentMapper healthAssessmentMapper;

    @PostMapping("/generate")
    public Result<Report> generate(@RequestBody Map<String, Object> dto) {
        String relatedType = String.valueOf(dto.getOrDefault("relatedType", "BATTERY"));
        Long relatedId = Long.parseLong(String.valueOf(dto.get("relatedId")));
        Long count = reportMapper.selectCount(new LambdaQueryWrapper<Report>()
                .eq(Report::getRelatedType, relatedType).eq(Report::getRelatedId, relatedId));
        String version = "v" + (count + 1L);
        String llmText = "此处为模拟实现，正式环境需替换。根据评估数据，该电池剩余寿命约XX年，推荐用于低速电动车或储能场景。";
        HealthAssessment latest = healthAssessmentMapper.selectOne(new LambdaQueryWrapper<HealthAssessment>()
                .eq(HealthAssessment::getBatteryId, relatedId).orderByDesc(HealthAssessment::getId).last("limit 1"));
        String content = llmText + (latest == null ? "" : ("；当前健康分:" + latest.getHealthScore() + "，等级:" + latest.getHealthLevel()));
        Report report = new Report();
        report.setRelatedType(relatedType);
        report.setRelatedId(relatedId);
        report.setVersionNo(version);
        report.setSummary("智能报告" + version);
        report.setContent(content);
        report.setCreatedBy(AuthUserContext.getCurrentUserId());
        reportMapper.insert(report);
        return Result.success(report);
    }

    @GetMapping("/list")
    public Result<List<Report>> list(@RequestParam(required = false) String relatedType, @RequestParam(required = false) Long relatedId) {
        LambdaQueryWrapper<Report> qw = new LambdaQueryWrapper<Report>();
        if (relatedType != null && !relatedType.isEmpty()) {
            qw.eq(Report::getRelatedType, relatedType);
        }
        if (relatedId != null) {
            qw.eq(Report::getRelatedId, relatedId);
        }
        qw.orderByDesc(Report::getId);
        return Result.success(reportMapper.selectList(qw));
    }

    @GetMapping("/{id}")
    public Result<Report> detail(@PathVariable Long id) {
        return Result.success(reportMapper.selectById(id));
    }

    @GetMapping("/compare")
    public Result<Map<String, Object>> compare(@RequestParam Long id1, @RequestParam Long id2) {
        Report r1 = reportMapper.selectById(id1);
        Report r2 = reportMapper.selectById(id2);
        String c1 = r1 == null ? "" : r1.getContent();
        String c2 = r2 == null ? "" : r2.getContent();
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("report1", r1);
        data.put("report2", r2);
        data.put("difference", simpleDiff(c1, c2));
        return Result.success(data);
    }

    private List<String> simpleDiff(String a, String b) {
        Set<String> sa = new HashSet<String>(Arrays.asList(a.split("；")));
        Set<String> sb = new HashSet<String>(Arrays.asList(b.split("；")));
        List<String> out = new ArrayList<String>();
        for (String s : sa) {
            if (!sb.contains(s) && !s.trim().isEmpty()) {
                out.add("- " + s);
            }
        }
        for (String s : sb) {
            if (!sa.contains(s) && !s.trim().isEmpty()) {
                out.add("+ " + s);
            }
        }
        return out;
    }
}
