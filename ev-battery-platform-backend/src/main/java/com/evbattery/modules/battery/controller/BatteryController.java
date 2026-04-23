package com.evbattery.modules.battery.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.evbattery.common.AuthUserContext;
import com.evbattery.common.result.Result;
import com.evbattery.modules.battery.dto.BatteryBatchDeleteDTO;
import com.evbattery.modules.battery.dto.BatteryBatchTagDTO;
import com.evbattery.modules.battery.dto.BatteryDraftDTO;
import com.evbattery.modules.battery.dto.BatteryManualDTO;
import com.evbattery.modules.battery.dto.BatteryTagAssignDTO;
import com.evbattery.modules.battery.entity.BatteryDraft;
import com.evbattery.modules.battery.entity.BatteryRecord;
import com.evbattery.modules.battery.entity.Tag;
import com.evbattery.modules.battery.mapper.BatteryDraftMapper;
import com.evbattery.modules.battery.mapper.BatteryRecordMapper;
import com.evbattery.modules.battery.mapper.TagMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Locale;

@RestController
@RequestMapping("/api/battery")
public class BatteryController {

    private static final List<String> ALLOWED_STATUS = Arrays.asList(
            "PENDING_ASSESSMENT", "ASSESSED", "TRADED", "OFFLINE", "DRAFT"
    );

    @Resource
    private BatteryRecordMapper batteryRecordMapper;
    @Resource
    private BatteryDraftMapper batteryDraftMapper;
    @Resource
    private TagMapper tagMapper;
    @Resource
    private JdbcTemplate jdbcTemplate;
    @Resource
    private ObjectMapper objectMapper;

    @PostMapping("/upload/single")
    public Result<Map<String, Object>> uploadSingle(@RequestParam("file") MultipartFile file) {
        try {
            return Result.success("上传成功", saveFileAsBattery(file));
        } catch (IllegalArgumentException ex) {
            return Result.fail(400, ex.getMessage());
        }
    }

    @PostMapping("/upload/batch")
    public Result<Map<String, Object>> uploadBatch(@RequestParam("files") MultipartFile[] files) {
        List<Map<String, Object>> successItems = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> failedItems = new ArrayList<Map<String, Object>>();
        for (MultipartFile file : files) {
            try {
                successItems.add(saveFileAsBattery(file));
            } catch (IllegalArgumentException ex) {
                Map<String, Object> item = new LinkedHashMap<String, Object>();
                item.put("fileName", file == null ? null : file.getOriginalFilename());
                item.put("message", ex.getMessage());
                failedItems.add(item);
            }
        }
        if (successItems.isEmpty()) {
            return Result.fail(400, failedItems.isEmpty() ? "批量上传失败" : String.valueOf(failedItems.get(0).get("message")));
        }
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("successCount", successItems.size());
        data.put("failCount", failedItems.size());
        data.put("records", successItems);
        data.put("failed", failedItems);
        return Result.success(failedItems.isEmpty() ? "批量上传成功" : "部分文件已导入，其余文件导入失败", data);
    }

    @PostMapping("/manual")
    public Result<Map<String, Object>> createManual(@RequestBody BatteryManualDTO dto) {
        BatteryRecord record = saveManualRecord(dto, currentUserId(), true);
        return Result.success("创建成功", buildBatteryDetail(record.getId()));
    }

    @PutMapping("/{id}")
    public Result<Map<String, Object>> update(@PathVariable Long id, @RequestBody BatteryManualDTO dto) {
        BatteryRecord record = getOwnedBattery(id);
        if (record == null) {
            return Result.fail(404, "电池档案不存在");
        }
        mergeRecord(record, dto);
        batteryRecordMapper.updateById(record);
        if (dto.getTagIds() != null || dto.getTagNames() != null) {
            replaceBatteryTags(record.getId(), dto.getTagIds(), dto.getTagNames());
        }
        return Result.success("更新成功", buildBatteryDetail(id));
    }

    @PatchMapping("/{id}/status")
    public Result<String> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        BatteryRecord record = getOwnedBattery(id);
        if (record == null) {
            return Result.fail(404, "电池档案不存在");
        }
        record.setStatus(normalizeStatus(body == null ? null : body.get("status")));
        batteryRecordMapper.updateById(record);
        return Result.success("状态已更新", null);
    }

    @DeleteMapping("/{id}")
    public Result<String> deleteOne(@PathVariable Long id) {
        BatteryRecord record = getOwnedBattery(id);
        if (record == null) {
            return Result.fail(404, "电池档案不存在");
        }
        record.setIsDeleted(Boolean.TRUE);
        batteryRecordMapper.updateById(record);
        return Result.success("删除成功", null);
    }

    @DeleteMapping("/batch")
    public Result<Map<String, Object>> deleteBatch(@RequestBody BatteryBatchDeleteDTO dto) {
        List<Long> ids = dto == null ? null : dto.getIds();
        if (ids == null || ids.isEmpty()) {
            return Result.fail(400, "请选择需要删除的记录");
        }
        int affected = jdbcTemplate.update(
                "update battery_record set is_deleted = 1 where created_by = ? and id in (" + placeholders(ids.size()) + ")",
                joinArgs(currentUserId(), ids)
        );
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("count", affected);
        return Result.success("批量删除成功", data);
    }

    @PutMapping("/batch/tag")
    public Result<String> batchTag(@RequestBody BatteryBatchTagDTO dto) {
        if (dto == null || dto.getIds() == null || dto.getIds().isEmpty()) {
            return Result.fail(400, "请选择需要打标签的记录");
        }
        List<Long> tagIds = resolveTagIds(dto.getTagIds(), dto.getTagNames());
        if (tagIds.isEmpty()) {
            return Result.fail(400, "请选择标签");
        }
        for (Long id : dto.getIds()) {
            BatteryRecord record = getOwnedBattery(id);
            if (record != null) {
                attachTags(id, tagIds);
            }
        }
        return Result.success("批量打标签成功", null);
    }

    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "12") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<String> sourceTypes,
            @RequestParam(required = false) List<String> statuses,
            @RequestParam(required = false) BigDecimal minVoltage,
            @RequestParam(required = false) BigDecimal maxVoltage,
            @RequestParam(required = false) BigDecimal minResistance,
            @RequestParam(required = false) BigDecimal maxResistance,
            @RequestParam(required = false) Integer minCycleCount,
            @RequestParam(required = false) Integer maxCycleCount,
            @RequestParam(required = false) BigDecimal minTemperature,
            @RequestParam(required = false) BigDecimal maxTemperature,
            @RequestParam(required = false) List<Long> tagIds,
            @RequestParam(defaultValue = "OR") String tagMode,
            @RequestParam(required = false) String createdFrom,
            @RequestParam(required = false) String createdTo,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        List<Object> args = new ArrayList<Object>();
        StringBuilder where = new StringBuilder(" where br.is_deleted = 0 and br.created_by = ? ");
        args.add(currentUserId());
        if (StringUtils.hasText(keyword)) {
            where.append(" and br.battery_code like ? ");
            args.add("%" + keyword.trim() + "%");
        }
        if (sourceTypes != null && !sourceTypes.isEmpty()) {
            where.append(" and br.source_type in (").append(placeholders(sourceTypes.size())).append(") ");
            args.addAll(sourceTypes);
        }
        if (statuses != null && !statuses.isEmpty()) {
            where.append(" and br.status in (").append(placeholders(statuses.size())).append(") ");
            args.addAll(statuses);
        }
        if (minVoltage != null) {
            where.append(" and br.voltage >= ? ");
            args.add(minVoltage);
        }
        if (maxVoltage != null) {
            where.append(" and br.voltage <= ? ");
            args.add(maxVoltage);
        }
        if (minResistance != null) {
            where.append(" and br.internal_resistance_ratio >= ? ");
            args.add(minResistance);
        }
        if (maxResistance != null) {
            where.append(" and br.internal_resistance_ratio <= ? ");
            args.add(maxResistance);
        }
        if (minCycleCount != null) {
            where.append(" and br.cycle_count >= ? ");
            args.add(minCycleCount);
        }
        if (maxCycleCount != null) {
            where.append(" and br.cycle_count <= ? ");
            args.add(maxCycleCount);
        }
        if (minTemperature != null) {
            where.append(" and br.avg_temperature >= ? ");
            args.add(minTemperature);
        }
        if (maxTemperature != null) {
            where.append(" and br.avg_temperature <= ? ");
            args.add(maxTemperature);
        }
        if (StringUtils.hasText(createdFrom)) {
            where.append(" and br.created_at >= ? ");
            args.add(createdFrom.trim() + " 00:00:00");
        }
        if (StringUtils.hasText(createdTo)) {
            where.append(" and br.created_at <= ? ");
            args.add(createdTo.trim() + " 23:59:59");
        }
        appendTagFilter(where, args, tagIds, tagMode);

        Integer total = jdbcTemplate.queryForObject(
                "select count(1) from battery_record br " + where.toString(),
                Integer.class,
                args.toArray()
        );

        int currentPage = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 12 : Math.min(size, 100);
        List<Object> queryArgs = new ArrayList<Object>(args);
        queryArgs.add((currentPage - 1) * pageSize);
        queryArgs.add(pageSize);

        String sql = "select br.id as id, br.battery_code as batteryCode, br.source_type as sourceType, " +
                "br.bms_raw_file_path as bmsRawFilePath, br.feature_json as featureJson, br.audit_status as auditStatus, " +
                "br.status as status, br.remark as remark, br.created_by as createdBy, br.voltage as voltage, " +
                "br.capacity_retention_rate as capacityRetentionRate, br.internal_resistance_ratio as internalResistanceRatio, " +
                "br.cycle_count as cycleCount, br.avg_temperature as avgTemperature, br.created_at as createdAt, br.updated_at as updatedAt, " +
                "la.health_score as latestHealthScore, la.health_level as latestHealthLevel, la.assessment_time as latestAssessmentTime " +
                "from battery_record br left join (" + latestAssessmentSubquery() + ") la on la.battery_id = br.id " +
                where.toString() + " order by " + resolveSortColumn(sortBy) + " " + resolveSortOrder(sortOrder) + " limit ?, ?";
        List<Map<String, Object>> records = jdbcTemplate.queryForList(sql, queryArgs.toArray());
        fillTags(records);

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("records", records);
        data.put("total", total == null ? 0 : total);
        data.put("page", currentPage);
        data.put("size", pageSize);
        return Result.success(data);
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        BatteryRecord record = getOwnedBattery(id);
        if (record == null) {
            return Result.fail(404, "电池档案不存在");
        }
        return Result.success(buildBatteryDetail(id));
    }

    @PostMapping("/draft")
    public Result<Map<String, Object>> saveDraft(@RequestBody BatteryDraftDTO dto) {
        if (dto == null || dto.getDraftData() == null) {
            return Result.fail(400, "草稿内容不能为空");
        }
        BatteryDraft draft = dto.getId() == null ? new BatteryDraft() : batteryDraftMapper.selectById(dto.getId());
        if (draft != null && draft.getId() != null && !currentUserId().equals(draft.getUserId())) {
            return Result.fail(404, "草稿不存在");
        }
        if (draft == null) {
            draft = new BatteryDraft();
        }
        draft.setUserId(currentUserId());
        draft.setTitle(StringUtils.hasText(dto.getTitle()) ? dto.getTitle().trim() : generateDraftTitle());
        try {
            draft.setDraftData(objectMapper.writeValueAsString(dto.getDraftData()));
        } catch (Exception ex) {
            return Result.fail(400, "草稿内容格式不正确");
        }
        if (draft.getId() == null) {
            batteryDraftMapper.insert(draft);
        } else {
            batteryDraftMapper.updateById(draft);
        }
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("id", draft.getId());
        data.put("title", draft.getTitle());
        data.put("updatedAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return Result.success("草稿已保存", data);
    }

    @GetMapping("/draft/list")
    public Result<List<Map<String, Object>>> draftList() {
        List<BatteryDraft> drafts = batteryDraftMapper.selectList(new LambdaQueryWrapper<BatteryDraft>()
                .eq(BatteryDraft::getUserId, currentUserId())
                .orderByDesc(BatteryDraft::getUpdatedAt)
                .orderByDesc(BatteryDraft::getId));
        List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
        for (BatteryDraft draft : drafts) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("id", draft.getId());
            item.put("title", draft.getTitle());
            item.put("createdAt", draft.getCreatedAt());
            item.put("updatedAt", draft.getUpdatedAt());
            item.put("draftData", parseJsonObject(draft.getDraftData()));
            data.add(item);
        }
        return Result.success(data);
    }

    @DeleteMapping("/draft/{id}")
    public Result<String> deleteDraft(@PathVariable Long id) {
        BatteryDraft draft = batteryDraftMapper.selectById(id);
        if (draft == null || !currentUserId().equals(draft.getUserId())) {
            return Result.fail(404, "草稿不存在");
        }
        batteryDraftMapper.deleteById(id);
        return Result.success("草稿已删除", null);
    }

    @PostMapping("/{id}/tags")
    public Result<String> assignTags(@PathVariable Long id, @RequestBody BatteryTagAssignDTO dto) {
        BatteryRecord record = getOwnedBattery(id);
        if (record == null) {
            return Result.fail(404, "电池档案不存在");
        }
        List<Long> tagIds = resolveTagIds(dto == null ? null : dto.getTagIds(), dto == null ? null : dto.getTagNames());
        if (tagIds.isEmpty()) {
            return Result.fail(400, "请选择标签");
        }
        attachTags(id, tagIds);
        return Result.success("标签更新成功", null);
    }
    @DeleteMapping("/{id}/tags")
    public Result<String> deleteTags(@PathVariable Long id, @RequestBody(required = false) BatteryTagAssignDTO dto) {
        BatteryRecord record = getOwnedBattery(id);
        if (record == null) {
            return Result.fail(404, "电池档案不存在");
        }
        if (dto == null || ((dto.getTagIds() == null || dto.getTagIds().isEmpty()) && (dto.getTagNames() == null || dto.getTagNames().isEmpty()))) {
            jdbcTemplate.update("delete from battery_tag_relation where battery_id = ?", id);
            return Result.success("标签已清空", null);
        }
        List<Long> tagIds = resolveTagIds(dto.getTagIds(), dto.getTagNames());
        if (!tagIds.isEmpty()) {
            jdbcTemplate.update(
                    "delete from battery_tag_relation where battery_id = ? and tag_id in (" + placeholders(tagIds.size()) + ")",
                    joinArgs(id, tagIds)
            );
        }
        return Result.success("标签删除成功", null);
    }

    @GetMapping("/statistics")
    public Result<Map<String, Object>> statistics() {
        Long userId = currentUserId();
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        Integer total = jdbcTemplate.queryForObject("select count(1) from battery_record where is_deleted = 0 and created_by = ?", Integer.class, userId);
        data.put("total", total == null ? 0 : total);
        data.put("statusCounts", toCountMap(jdbcTemplate.queryForList(
                "select status as itemKey, count(1) as itemCount from battery_record where is_deleted = 0 and created_by = ? group by status",
                userId
        )));
        data.put("sourceCounts", toCountMap(jdbcTemplate.queryForList(
                "select source_type as itemKey, count(1) as itemCount from battery_record where is_deleted = 0 and created_by = ? group by source_type",
                userId
        )));
        Double avgScore = jdbcTemplate.query(
                "select avg(x.health_score) from (" + latestAssessmentSubquery() + ") x join battery_record br on br.id = x.battery_id where br.is_deleted = 0 and br.created_by = ?",
                rs -> rs.next() ? rs.getDouble(1) : null,
                userId
        );
        data.put("averageHealthScore", avgScore == null ? null : scale(avgScore));
        data.put("draftCount", jdbcTemplate.queryForObject("select count(1) from battery_draft where user_id = ?", Integer.class, userId));
        return Result.success(data);
    }

    private BatteryRecord saveManualRecord(BatteryManualDTO dto, Long userId, boolean deleteDraft) {
        BatteryRecord record = new BatteryRecord();
        record.setBatteryCode(generateBatteryCode());
        record.setCreatedBy(userId);
        record.setAuditStatus(0);
        mergeRecord(record, dto);
        batteryRecordMapper.insert(record);
        replaceBatteryTags(record.getId(), dto.getTagIds(), dto.getTagNames());
        if (deleteDraft && dto.getDraftId() != null) {
            BatteryDraft draft = batteryDraftMapper.selectById(dto.getDraftId());
            if (draft != null && userId.equals(draft.getUserId())) {
                batteryDraftMapper.deleteById(draft.getId());
            }
        }
        return record;
    }

    private void mergeRecord(BatteryRecord record, BatteryManualDTO dto) {
        record.setSourceType(StringUtils.hasText(dto.getSourceType()) ? dto.getSourceType().trim() : "手动录入");
        record.setRemark(StringUtils.hasText(dto.getRemark()) ? dto.getRemark().trim() : null);
        record.setVoltage(firstNonNull(dto.getVoltage(), record.getVoltage()));
        record.setCapacityRetentionRate(firstNonNull(dto.getCapacityRetentionRate(), record.getCapacityRetentionRate()));
        record.setInternalResistanceRatio(firstNonNull(dto.getInternalResistanceRatio(), record.getInternalResistanceRatio()));
        record.setCycleCount(dto.getCycleCount() == null ? record.getCycleCount() : dto.getCycleCount());
        record.setAvgTemperature(firstNonNull(dto.getAvgTemperature(), record.getAvgTemperature()));
        record.setStatus(normalizeStatus(dto.getStatus()));
        record.setFeatureJson(buildFeatureJson(record, dto.getExtraFeatures()));
        if (record.getIsDeleted() == null) {
            record.setIsDeleted(Boolean.FALSE);
        }
    }

    private String buildFeatureJson(BatteryRecord record, Map<String, Object> extraFeatures) {
        Map<String, Object> feature = new LinkedHashMap<String, Object>();
        if (extraFeatures != null) {
            feature.putAll(extraFeatures);
        }
        feature.put("voltage", record.getVoltage());
        feature.put("capacityRetentionRate", record.getCapacityRetentionRate());
        feature.put("internalResistanceRatio", record.getInternalResistanceRatio());
        feature.put("cycleCount", record.getCycleCount());
        feature.put("avgTemperature", record.getAvgTemperature());
        feature.put("status", record.getStatus());
        feature.put("remark", record.getRemark());
        try {
            return objectMapper.writeValueAsString(feature);
        } catch (Exception ex) {
            throw new RuntimeException("特征数据保存失败", ex);
        }
    }

    private Map<String, Object> buildBatteryDetail(Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select br.id as id, br.battery_code as batteryCode, br.source_type as sourceType, br.bms_raw_file_path as bmsRawFilePath, " +
                        "br.feature_json as featureJson, br.audit_status as auditStatus, br.status as status, br.remark as remark, " +
                        "br.created_by as createdBy, br.voltage as voltage, br.capacity_retention_rate as capacityRetentionRate, " +
                        "br.internal_resistance_ratio as internalResistanceRatio, br.cycle_count as cycleCount, br.avg_temperature as avgTemperature, " +
                        "br.created_at as createdAt, br.updated_at as updatedAt, la.health_score as latestHealthScore, " +
                        "la.health_level as latestHealthLevel, la.assessment_time as latestAssessmentTime " +
                        "from battery_record br left join (" + latestAssessmentSubquery() + ") la on la.battery_id = br.id " +
                        "where br.id = ? and br.is_deleted = 0 and br.created_by = ?",
                id, currentUserId()
        );
        if (rows.isEmpty()) {
            return null;
        }
        Map<String, Object> detail = rows.get(0);
        fillTags(Collections.singletonList(detail));
        detail.put("featureMap", parseJsonObject((String) detail.get("featureJson")));
        detail.put("latestAssessment", buildLatestAssessment(id));
        detail.put("timeline", buildTimeline(id, detail));
        detail.put("similarBatteries", findSimilarBatteries(id));
        return detail;
    }

    private Map<String, Object> buildLatestAssessment(Long batteryId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select id, health_score as healthScore, health_level as healthLevel, rule_score as ruleScore, ml_score as mlScore, suggested_scene as suggestedScene, llm_summary as llmSummary, assessment_time as assessmentTime from health_assessment where battery_id = ? order by id desc limit 1",
                batteryId
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    private List<Map<String, Object>> buildTimeline(Long batteryId, Map<String, Object> detail) {
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        items.add(event("档案创建", "创建电池档案", detail.get("createdAt")));
        if (detail.get("updatedAt") != null && !String.valueOf(detail.get("updatedAt")).equals(String.valueOf(detail.get("createdAt")))) {
            items.add(event("档案编辑", "更新电池档案", detail.get("updatedAt")));
        }
        List<Map<String, Object>> history = jdbcTemplate.queryForList(
                "select assessment_time as eventTime, health_score as healthScore, health_level as healthLevel from health_assessment where battery_id = ? order by id desc",
                batteryId
        );
        for (Map<String, Object> row : history) {
            items.add(event("健康评估", "评分 " + row.get("healthScore") + " / 等级 " + row.get("healthLevel"), row.get("eventTime")));
        }
        if ("TRADED".equals(String.valueOf(detail.get("status")))) {
            items.add(event("交易完成", "该电池已进入交易状态", detail.get("updatedAt")));
        }
        return items;
    }

    private Map<String, Object> event(String title, String description, Object time) {
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("title", title);
        item.put("description", description);
        item.put("time", time);
        return item;
    }

    private List<Map<String, Object>> findSimilarBatteries(Long currentId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select id, battery_code as batteryCode, source_type as sourceType, status, voltage, cycle_count as cycleCount, capacity_retention_rate as capacityRetentionRate, internal_resistance_ratio as internalResistanceRatio from battery_record where id <> ? and is_deleted = 0 and created_by = ? order by abs(cycle_count - (select cycle_count from battery_record where id = ?)) asc limit 4",
                currentId, currentUserId(), currentId
        );
        fillTags(rows);
        return rows;
    }

    private void appendTagFilter(StringBuilder where, List<Object> args, List<Long> tagIds, String tagMode) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        if ("AND".equalsIgnoreCase(tagMode)) {
            for (Long tagId : tagIds) {
                where.append(" and exists (select 1 from battery_tag_relation btr where btr.battery_id = br.id and btr.tag_id = ?) ");
                args.add(tagId);
            }
            return;
        }
        where.append(" and exists (select 1 from battery_tag_relation btr where btr.battery_id = br.id and btr.tag_id in (").append(placeholders(tagIds.size())).append(")) ");
        args.addAll(tagIds);
    }

    private String latestAssessmentSubquery() {
        return "select ha.* from health_assessment ha join (select battery_id, max(id) as max_id from health_assessment group by battery_id) latest on latest.max_id = ha.id";
    }

    private void fillTags(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List<Long> batteryIds = new ArrayList<Long>();
        for (Map<String, Object> row : rows) {
            batteryIds.add(longValue(row.get("id")));
        }
        Map<Long, List<Map<String, Object>>> tagMap = queryTagsByBatteryIds(batteryIds);
        for (Map<String, Object> row : rows) {
            Long id = longValue(row.get("id"));
            row.put("tags", tagMap.get(id) == null ? new ArrayList<Map<String, Object>>() : tagMap.get(id));
        }
    }

    private Map<Long, List<Map<String, Object>>> queryTagsByBatteryIds(List<Long> batteryIds) {
        if (batteryIds == null || batteryIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select btr.battery_id as batteryId, t.id as id, t.name as name, t.color as color from battery_tag_relation btr join tag t on t.id = btr.tag_id where btr.battery_id in (" + placeholders(batteryIds.size()) + ")",
                batteryIds.toArray()
        );
        Map<Long, List<Map<String, Object>>> map = new HashMap<Long, List<Map<String, Object>>>();
        for (Map<String, Object> row : rows) {
            Long batteryId = longValue(row.get("batteryId"));
            List<Map<String, Object>> tags = map.get(batteryId);
            if (tags == null) {
                tags = new ArrayList<Map<String, Object>>();
                map.put(batteryId, tags);
            }
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("id", row.get("id"));
            item.put("name", row.get("name"));
            item.put("color", row.get("color"));
            tags.add(item);
        }
        return map;
    }

    private void replaceBatteryTags(Long batteryId, List<Long> tagIds, List<String> tagNames) {
        jdbcTemplate.update("delete from battery_tag_relation where battery_id = ?", batteryId);
        attachTags(batteryId, resolveTagIds(tagIds, tagNames));
    }

    private void attachTags(Long batteryId, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        for (Long tagId : tagIds) {
            Integer count = jdbcTemplate.queryForObject(
                    "select count(1) from battery_tag_relation where battery_id = ? and tag_id = ?",
                    Integer.class,
                    batteryId, tagId
            );
            if (count == null || count == 0) {
                jdbcTemplate.update("insert into battery_tag_relation(battery_id, tag_id) values(?, ?)", batteryId, tagId);
            }
        }
    }

    private List<Long> resolveTagIds(List<Long> tagIds, List<String> tagNames) {
        List<Long> result = new ArrayList<Long>();
        if (tagIds != null) {
            result.addAll(tagIds);
        }
        if (tagNames != null) {
            for (String name : tagNames) {
                if (!StringUtils.hasText(name)) {
                    continue;
                }
                Tag tag = tagMapper.selectOne(new LambdaQueryWrapper<Tag>().eq(Tag::getName, name.trim()));
                if (tag == null) {
                    tag = new Tag();
                    tag.setName(name.trim());
                    tag.setColor(randomTagColor());
                    tagMapper.insert(tag);
                }
                if (!result.contains(tag.getId())) {
                    result.add(tag.getId());
                }
            }
        }
        return result;
    }

    private Map<String, Object> toCountMap(List<Map<String, Object>> rows) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        for (Map<String, Object> row : rows) {
            result.put(String.valueOf(row.get("itemKey")), row.get("itemCount"));
        }
        return result;
    }

    private BatteryRecord getOwnedBattery(Long id) {
        return batteryRecordMapper.selectOne(new LambdaQueryWrapper<BatteryRecord>()
                .eq(BatteryRecord::getId, id)
                .eq(BatteryRecord::getCreatedBy, currentUserId())
                .eq(BatteryRecord::getIsDeleted, Boolean.FALSE)
                .last("limit 1"));
    }

    private Map<String, Object> saveFileAsBattery(MultipartFile file) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
            List<String> lines = new ArrayList<String>();
            String line;
            while ((line = reader.readLine()) != null && lines.size() < 5) {
                if (!line.trim().isEmpty()) {
                    lines.add(line.trim());
                }
            }
            if (lines.isEmpty()) {
                throw new IllegalArgumentException("CSV文件为空");
            }
            BatteryManualDTO dto = new BatteryManualDTO();
            List<String> warnings = new ArrayList<String>();
            dto.setSourceType("上传导入");
            dto.setRemark(file.getOriginalFilename());
            dto.setStatus("PENDING_ASSESSMENT");
            if (lines.get(0).contains(",")) {
                String firstLineLower = lines.get(0).toLowerCase(Locale.ROOT);
                boolean hasHeader = firstLineLower.matches(".*[a-z\\u4e00-\\u9fa5].*");
                if (hasHeader) {
                    applyByHeader(lines, dto, warnings);
                } else {
                    String[] arr = lines.get(0).split(",");
                    applyBySequence(arr, dto, warnings);
                }
            }
            List<String> recognizedFields = collectRecognizedFields(dto);
            if (recognizedFields.isEmpty()) {
                throw new IllegalArgumentException("未识别到可用的电池关键数据，请检查文件表头或内容格式");
            }
            List<String> missingFields = collectMissingAssessmentFields(dto);
            if (!missingFields.isEmpty()) {
                warnings.add("缺少评估字段: " + String.join("、", missingFields));
            }
            Map<String, Object> extra = new HashMap<String, Object>();
            extra.put("preview", lines);
            extra.put("fileName", file.getOriginalFilename());
            extra.put("recognizedFields", recognizedFields);
            extra.put("missingFields", missingFields);
            extra.put("warnings", warnings);
            dto.setExtraFeatures(extra);
            BatteryRecord record = saveManualRecord(dto, currentUserId(), false);
            record.setBmsRawFilePath("/uploads/" + file.getOriginalFilename());
            batteryRecordMapper.updateById(record);
            Map<String, Object> data = new LinkedHashMap<String, Object>();
            data.put("id", record.getId());
            data.put("batteryCode", record.getBatteryCode());
            data.put("warnings", warnings);
            data.put("canAssess", missingFields.isEmpty());
            return data;
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("文件解析失败: " + ex.getMessage(), ex);
        }
    }

    private void applyByHeader(List<String> lines, BatteryManualDTO dto, List<String> warnings) {
        if (lines.size() < 2) {
            throw new IllegalArgumentException("CSV缺少数据行");
        }
        String[] headers = lines.get(0).split(",");
        String[] values = lines.get(1).split(",");
        Map<String, Integer> indexMap = new HashMap<String, Integer>();
        for (int i = 0; i < headers.length; i++) {
            indexMap.put(normalizeHeader(headers[i]), i);
        }
        Integer voltageIndex = optionalIndex(indexMap, "voltage", "电压", "v");
        Integer resistanceIndex = optionalIndex(indexMap, "internalresistance", "内阻", "resistance", "ir");
        Integer cycleIndex = optionalIndex(indexMap, "cyclecount", "循环", "cycle", "cycles");
        Integer tempIndex = optionalIndex(indexMap, "temperature", "温度", "temp");
        Integer capacityIndex = optionalIndex(indexMap, "capacityretentionrate", "容量", "capacity");
        applyDecimalValue(dto, values, voltageIndex, "电压", warnings, new DecimalSetter() {
            @Override
            public void apply(BigDecimal value) {
                dto.setVoltage(value);
            }
        });
        applyDecimalValue(dto, values, capacityIndex, "容量保持率", warnings, new DecimalSetter() {
            @Override
            public void apply(BigDecimal value) {
                dto.setCapacityRetentionRate(value);
            }
        });
        applyDecimalValue(dto, values, resistanceIndex, "内阻比", warnings, new DecimalSetter() {
            @Override
            public void apply(BigDecimal value) {
                dto.setInternalResistanceRatio(value);
            }
        });
        applyIntegerValue(dto, values, cycleIndex, "循环次数", warnings);
        applyDecimalValue(dto, values, tempIndex, "平均温度", warnings, new DecimalSetter() {
            @Override
            public void apply(BigDecimal value) {
                dto.setAvgTemperature(value);
            }
        });
    }

    private String normalizeHeader(String header) {
        return header == null ? "" : header.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "").replace(" ", "");
    }

    private void applyBySequence(String[] values, BatteryManualDTO dto, List<String> warnings) {
        if (values.length > 0) {
            double parsed = parseDouble(values[0], Double.NaN);
            if (!Double.isNaN(parsed)) {
                dto.setVoltage(scale(parsed));
            }
        }
        if (values.length > 1) {
            double parsed = parseDouble(values[1], Double.NaN);
            if (!Double.isNaN(parsed)) {
                dto.setCapacityRetentionRate(scale(parsed));
            }
        }
        if (values.length > 2) {
            double parsed = parseDouble(values[2], Double.NaN);
            if (!Double.isNaN(parsed)) {
                dto.setInternalResistanceRatio(scale(parsed));
            }
        }
        if (values.length > 3) {
            double cycleValue = parseDouble(values[3], Double.NaN);
            if (!Double.isNaN(cycleValue)) {
                dto.setCycleCount((int) Math.round(cycleValue));
            }
        }
        if (values.length > 4) {
            double parsed = parseDouble(values[4], Double.NaN);
            if (!Double.isNaN(parsed)) {
                dto.setAvgTemperature(scale(parsed));
            }
        }
        if (dto.getVoltage() == null && dto.getCapacityRetentionRate() == null && dto.getInternalResistanceRatio() == null
                && dto.getCycleCount() == null && dto.getAvgTemperature() == null) {
            warnings.add("未从首行顺序数据中识别到有效电池字段");
        }
    }

    private Integer optionalIndex(Map<String, Integer> indexMap, String... keys) {
        for (String key : keys) {
            String normalized = normalizeHeader(key);
            for (Map.Entry<String, Integer> entry : indexMap.entrySet()) {
                if (entry.getKey().contains(normalized)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private double parseDouble(String text, double defaultValue) {
        try {
            return Double.parseDouble(text.trim());
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    private void applyDecimalValue(BatteryManualDTO dto, String[] values, Integer index, String fieldName, List<String> warnings, DecimalSetter setter) {
        if (index == null || index >= values.length) {
            warnings.add(fieldName + "缺失");
            return;
        }
        double parsed = parseDouble(values[index], Double.NaN);
        if (Double.isNaN(parsed)) {
            warnings.add(fieldName + "无有效数值");
            return;
        }
        setter.apply(scale(parsed));
    }

    private void applyIntegerValue(BatteryManualDTO dto, String[] values, Integer index, String fieldName, List<String> warnings) {
        if (index == null || index >= values.length) {
            warnings.add(fieldName + "缺失");
            return;
        }
        double parsed = parseDouble(values[index], Double.NaN);
        if (Double.isNaN(parsed)) {
            warnings.add(fieldName + "无有效数值");
            return;
        }
        dto.setCycleCount((int) Math.round(parsed));
    }

    private BigDecimal scale(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal firstNonNull(BigDecimal incoming, BigDecimal existing) {
        return incoming != null ? incoming : existing;
    }

    private List<String> collectRecognizedFields(BatteryManualDTO dto) {
        List<String> fields = new ArrayList<String>();
        if (dto.getVoltage() != null) fields.add("电压");
        if (dto.getCapacityRetentionRate() != null) fields.add("容量保持率");
        if (dto.getInternalResistanceRatio() != null) fields.add("内阻比");
        if (dto.getCycleCount() != null) fields.add("循环次数");
        if (dto.getAvgTemperature() != null) fields.add("平均温度");
        return fields;
    }

    private List<String> collectMissingAssessmentFields(BatteryManualDTO dto) {
        List<String> fields = new ArrayList<String>();
        if (dto.getCapacityRetentionRate() == null) fields.add("容量保持率");
        if (dto.getInternalResistanceRatio() == null) fields.add("内阻比");
        if (dto.getCycleCount() == null) fields.add("循环次数");
        if (dto.getAvgTemperature() == null) fields.add("平均温度");
        return fields;
    }

    private interface DecimalSetter {
        void apply(BigDecimal value);
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return "PENDING_ASSESSMENT";
        }
        String value = status.trim().toUpperCase();
        return ALLOWED_STATUS.contains(value) ? value : "PENDING_ASSESSMENT";
    }

    private String resolveSortColumn(String sortBy) {
        if ("healthScore".equalsIgnoreCase(sortBy)) {
            return "la.health_score";
        }
        if ("cycleCount".equalsIgnoreCase(sortBy)) {
            return "br.cycle_count";
        }
        if ("voltage".equalsIgnoreCase(sortBy)) {
            return "br.voltage";
        }
        if ("capacityRetentionRate".equalsIgnoreCase(sortBy)) {
            return "br.capacity_retention_rate";
        }
        return "br.created_at";
    }

    private String resolveSortOrder(String sortOrder) {
        return "asc".equalsIgnoreCase(sortOrder) ? "asc" : "desc";
    }

    private String placeholders(int size) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append("?");
        }
        return builder.toString();
    }

    private Object[] joinArgs(Object firstArg, List<Long> ids) {
        Object[] args = new Object[ids.size() + 1];
        args[0] = firstArg;
        for (int i = 0; i < ids.size(); i++) {
            args[i + 1] = ids.get(i);
        }
        return args;
    }

    private Long currentUserId() {
        return AuthUserContext.getCurrentUserId() == null ? 1L : AuthUserContext.getCurrentUserId();
    }

    private String generateBatteryCode() {
        return "EVB-" + System.currentTimeMillis() + "-" + ThreadLocalRandom.current().nextInt(100, 999);
    }

    private String generateDraftTitle() {
        return "未完成的电池档案-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private Map<String, Object> parseJsonObject(String json) {
        if (!StringUtils.hasText(json)) {
            return new LinkedHashMap<String, Object>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            return new LinkedHashMap<String, Object>();
        }
    }

    private Long longValue(Object value) {
        return value == null ? null : Long.valueOf(String.valueOf(value));
    }

    private String randomTagColor() {
        String[] colors = {"#409EFF", "#67C23A", "#E6A23C", "#F56C6C", "#909399", "#14B8A6", "#7C3AED"};
        return colors[ThreadLocalRandom.current().nextInt(colors.length)];
    }
}
