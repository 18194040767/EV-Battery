package com.evbattery.config;

import com.evbattery.modules.assessment.service.RuleScoreService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class DataInitializer implements ApplicationRunner {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private RuleScoreService ruleScoreService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            ensureSchemaUpdates();
            initRoles();
            initUsers();
            initPermissions();
            initDemoBatteries();
            initDemoAssessments();
            initDemoProfiles();
            initDemoAddresses();
            initDemoProducts();
        } catch (Exception e) {
            System.err.println("Data initialization failed (this is acceptable during development): " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void ensureSchemaUpdates() {
        ensureColumn("battery_record", "status", "ALTER TABLE battery_record ADD COLUMN status VARCHAR(30) DEFAULT 'PENDING_ASSESSMENT'");
        ensureColumn("battery_record", "remark", "ALTER TABLE battery_record ADD COLUMN remark VARCHAR(255)");
        ensureColumn("battery_record", "is_deleted", "ALTER TABLE battery_record ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE");
        ensureColumn("battery_record", "voltage", "ALTER TABLE battery_record ADD COLUMN voltage DECIMAL(6,2)");
        ensureIndex("battery_record", "idx_battery_status", "CREATE INDEX idx_battery_status ON battery_record(status)");
        ensureIndex("battery_record", "idx_battery_source", "CREATE INDEX idx_battery_source ON battery_record(source_type)");
        ensureIndex("battery_record", "idx_battery_deleted", "CREATE INDEX idx_battery_deleted ON battery_record(is_deleted)");

        ensureColumn("product", "original_price", "ALTER TABLE product ADD COLUMN original_price DECIMAL(12,2)");
        ensureColumn("product", "cover_image", "ALTER TABLE product ADD COLUMN cover_image VARCHAR(255)");
        ensureColumn("product", "image_urls", "ALTER TABLE product ADD COLUMN image_urls JSON");
        ensureColumn("product", "shipping_from", "ALTER TABLE product ADD COLUMN shipping_from VARCHAR(100)");
        ensureColumn("product", "shipping_type", "ALTER TABLE product ADD COLUMN shipping_type VARCHAR(30)");
        ensureColumn("product", "battery_type", "ALTER TABLE product ADD COLUMN battery_type VARCHAR(50)");
        ensureColumn("product", "health_level", "ALTER TABLE product ADD COLUMN health_level VARCHAR(30)");
        ensureColumn("product", "is_free_shipping", "ALTER TABLE product ADD COLUMN is_free_shipping BOOLEAN DEFAULT TRUE");
        ensureColumn("product", "sale_count", "ALTER TABLE product ADD COLUMN sale_count INT DEFAULT 0");
        ensureColumn("product", "view_count", "ALTER TABLE product ADD COLUMN view_count INT DEFAULT 0");
        ensureColumn("product", "favorite_count", "ALTER TABLE product ADD COLUMN favorite_count INT DEFAULT 0");
        ensureColumn("product", "publish_status", "ALTER TABLE product ADD COLUMN publish_status VARCHAR(30) DEFAULT 'PENDING_REVIEW'");
        ensureColumn("product", "audit_status", "ALTER TABLE product ADD COLUMN audit_status VARCHAR(30) DEFAULT 'APPROVED'");
        ensureColumn("product", "draft_flag", "ALTER TABLE product ADD COLUMN draft_flag BOOLEAN DEFAULT FALSE");
        ensureColumn("product", "deleted_flag", "ALTER TABLE product ADD COLUMN deleted_flag BOOLEAN DEFAULT FALSE");

        ensureColumn("`order`", "quantity", "ALTER TABLE `order` ADD COLUMN quantity INT DEFAULT 1");
        ensureColumn("`order`", "unit_price", "ALTER TABLE `order` ADD COLUMN unit_price DECIMAL(12,2)");
        ensureColumn("`order`", "address_snapshot", "ALTER TABLE `order` ADD COLUMN address_snapshot JSON");
        ensureColumn("`order`", "product_snapshot", "ALTER TABLE `order` ADD COLUMN product_snapshot JSON");
        ensureColumn("`order`", "payment_method", "ALTER TABLE `order` ADD COLUMN payment_method VARCHAR(30)");
        ensureColumn("`order`", "buyer_deleted", "ALTER TABLE `order` ADD COLUMN buyer_deleted BOOLEAN DEFAULT FALSE");
        ensureColumn("`order`", "seller_deleted", "ALTER TABLE `order` ADD COLUMN seller_deleted BOOLEAN DEFAULT FALSE");
        ensureColumn("`order`", "pay_time", "ALTER TABLE `order` ADD COLUMN pay_time DATETIME");
        ensureColumn("`order`", "ship_time", "ALTER TABLE `order` ADD COLUMN ship_time DATETIME");
        ensureColumn("`order`", "receive_time", "ALTER TABLE `order` ADD COLUMN receive_time DATETIME");
        ensureColumn("`order`", "complete_time", "ALTER TABLE `order` ADD COLUMN complete_time DATETIME");
        ensureColumn("`order`", "cancel_time", "ALTER TABLE `order` ADD COLUMN cancel_time DATETIME");
        ensureColumn("`order`", "refund_time", "ALTER TABLE `order` ADD COLUMN refund_time DATETIME");
        ensureColumn("`order`", "logistics_company", "ALTER TABLE `order` ADD COLUMN logistics_company VARCHAR(100)");
        ensureColumn("`order`", "logistics_no", "ALTER TABLE `order` ADD COLUMN logistics_no VARCHAR(100)");

        ensureColumn("contract", "contract_no", "ALTER TABLE contract ADD COLUMN contract_no VARCHAR(64)");
        ensureColumn("contract", "content_hash", "ALTER TABLE contract ADD COLUMN content_hash VARCHAR(128)");
        ensureColumn("contract", "pdf_hash", "ALTER TABLE contract ADD COLUMN pdf_hash VARCHAR(128)");
        ensureColumn("contract", "verify_count", "ALTER TABLE contract ADD COLUMN verify_count INT DEFAULT 0");

        ensureColumn("logistics_info", "notice_pdf_path", "ALTER TABLE logistics_info ADD COLUMN notice_pdf_path VARCHAR(255)");
        ensureColumn("logistics_info", "contact_name", "ALTER TABLE logistics_info ADD COLUMN contact_name VARCHAR(100)");
        ensureColumn("logistics_info", "contact_phone", "ALTER TABLE logistics_info ADD COLUMN contact_phone VARCHAR(30)");
        ensureColumn("logistics_info", "last_updated_at", "ALTER TABLE logistics_info ADD COLUMN last_updated_at DATETIME");
    }

    private void ensureColumn(String tableName, String columnName, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(1) from information_schema.columns where table_schema = database() and table_name = ? and column_name = ?",
                Integer.class,
                tableName.replace("`", ""),
                columnName
        );
        if (count == null || count == 0) {
            jdbcTemplate.execute(ddl);
        }
    }

    private void ensureIndex(String tableName, String indexName, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(1) from information_schema.statistics where table_schema = database() and table_name = ? and index_name = ?",
                Integer.class,
                tableName.replace("`", ""),
                indexName
        );
        if (count == null || count == 0) {
            jdbcTemplate.execute(ddl);
        }
    }

    private void initRoles() {
        upsertRole("ROLE_ADMIN", "管理员");
        upsertRole("ROLE_USER", "普通用户");
    }

    private void initUsers() {
        migrateLegacyTestUser();
        upsertUser("admin", "123456", "平台管理员", "admin@ev.com", "ROLE_ADMIN");
        upsertUser("test", "123456", "测试用户", "test@ev.com", "ROLE_USER");
        upsertUser("buyer01", "123456", "采购用户01", "buyer01@ev.com", "ROLE_USER");
        upsertUser("seller01", "123456", "销售用户01", "seller01@ev.com", "ROLE_USER");
    }

    private void migrateLegacyTestUser() {
        Integer oldCount = jdbcTemplate.queryForObject("select count(1) from `user` where username = 'testuser'", Integer.class);
        Integer newCount = jdbcTemplate.queryForObject("select count(1) from `user` where username = 'test'", Integer.class);
        if (oldCount != null && oldCount > 0 && (newCount == null || newCount == 0)) {
            jdbcTemplate.update("update `user` set username = 'test' where username = 'testuser'");
        }
    }

    private void initPermissions() {
        upsertPermission("user:list", "用户列表", "/api/admin/users");
        upsertPermission("battery:upload", "电池上传", "/api/battery/upload/single");
        upsertPermission("trade:order", "订单操作", "/api/trade/order/place");
    }

    private void initDemoBatteries() {
        Integer count = jdbcTemplate.queryForObject("select count(1) from battery_record", Integer.class);
        if (count != null && count > 0) {
            return;
        }
        List<Object[]> batteries = Arrays.asList(
                new Object[]{"EVB-DEMO-001", "4S店", "/mock/demo1.csv", 96.00, 0.08, 220, 26.50, 1L},
                new Object[]{"EVB-DEMO-002", "换电站", "/mock/demo2.csv", 86.00, 0.22, 780, 29.00, 1L},
                new Object[]{"EVB-DEMO-003", "维修中心", "/mock/demo3.csv", 74.00, 0.35, 1380, 31.50, 1L},
                new Object[]{"EVB-DEMO-004", "梯次仓储", "/mock/demo4.csv", 58.00, 0.55, 2150, 37.80, 1L}
        );
        for (Object[] item : batteries) {
            String featureJson = String.format(
                    "{\"capacityRetentionRate\":%.2f,\"internalResistanceRatio\":%.2f,\"cycleCount\":%d,\"avgTemperature\":%.2f}",
                    item[3], item[4], ((Number) item[5]).intValue(), item[6]
            );
            jdbcTemplate.update(
                    "insert into battery_record(battery_code,source_type,bms_raw_file_path,feature_json,audit_status,created_by,capacity_retention_rate,internal_resistance_ratio,cycle_count,avg_temperature) values(?,?,?,?,?,?,?,?,?,?)",
                    item[0], item[1], item[2], featureJson, 1, item[7], item[3], item[4], item[5], item[6]
            );
        }
    }

    private void initDemoAssessments() {
        Integer count = jdbcTemplate.queryForObject("select count(1) from health_assessment", Integer.class);
        if (count != null && count > 0) {
            return;
        }
        List<Long> batteryIds = jdbcTemplate.queryForList("select id from battery_record order by id", Long.class);
        for (Long batteryId : batteryIds) {
            BatterySeed seed = jdbcTemplate.queryForObject(
                    "select capacity_retention_rate, internal_resistance_ratio, cycle_count, avg_temperature from battery_record where id=?",
                    (rs, rowNum) -> new BatterySeed(
                            rs.getDouble("capacity_retention_rate"),
                            rs.getDouble("internal_resistance_ratio"),
                            rs.getInt("cycle_count"),
                            rs.getDouble("avg_temperature")
                    ),
                    batteryId
            );
            int ruleScore = ruleScoreService.calculateScore(seed.cap, seed.ir, seed.cycle, seed.temp);
            String level = ruleScoreService.resolveHealthLevel(ruleScore);
            String scene = ruleScoreService.resolveSuggestedScene(level);
            String trendData = String.format(
                    "[{\"month\":\"2025-11\",\"retention\":%.2f},{\"month\":\"2025-12\",\"retention\":%.2f},{\"month\":\"2026-01\",\"retention\":%.2f},{\"month\":\"2026-02\",\"retention\":%.2f},{\"month\":\"2026-03\",\"retention\":%.2f},{\"month\":\"2026-04\",\"retention\":%.2f}]",
                    rounded(seed.cap + 6), rounded(seed.cap + 5), rounded(seed.cap + 4), rounded(seed.cap + 3), rounded(seed.cap + 2), rounded(seed.cap)
            );
            String llmSummary = String.format(
                    "健康评分%d，等级%s，容量保持率%.2f%%，内阻增幅%.2f，循环%d次，平均温度%.2f℃，建议场景%s，剩余寿命%s。",
                    ruleScore, level, seed.cap, seed.ir, seed.cycle, seed.temp, scene, resolveLife(level)
            );
            jdbcTemplate.update(
                    "insert into health_assessment(battery_id,health_score,health_level,rule_score,ml_score,suggested_scene,trend_data,llm_summary,assessment_time,is_ml_enhanced,created_at,updated_at) values(?,?,?,?,?,?,?,?,?,?,?,?)",
                    batteryId, ruleScore, level, ruleScore, null, scene, trendData, llmSummary, LocalDateTime.now(), false, LocalDateTime.now(), LocalDateTime.now()
            );
            jdbcTemplate.update("update battery_record set status = 'ASSESSED' where id = ?", batteryId);
        }
    }

    private void initDemoProfiles() {
        upsertProfile("test", "演示采购员", "https://dummyimage.com/120x120/e8eef8/4a6480&text=T", "负责平台模拟采购与验收。", "上海");
        upsertProfile("buyer01", "采购经理-林楠", "https://dummyimage.com/120x120/dce7e5/1e3a38&text=B1", "关注梯次利用项目的批量采购与履约跟踪。", "苏州");
        upsertProfile("seller01", "供应商-启储能源", "https://dummyimage.com/120x120/e8f3ff/255b7f&text=S1", "提供通过检测和评估的退役动力电池。", "深圳");
    }

    private void initDemoAddresses() {
        ensureAddress("test", "平台演示收货人", "13800000000", "上海市", "上海市", "浦东新区", "世纪大道 1888 号", true);
        ensureAddress("buyer01", "苏州示范项目组", "13900000001", "江苏省", "苏州市", "工业园区", "金鸡湖大道 66 号", true);
        ensureAddress("seller01", "深圳仓配中心", "13700000002", "广东省", "深圳市", "宝安区", "航城大道 18 号", true);
    }

    private void initDemoProducts() {
        Integer count = jdbcTemplate.queryForObject("select count(1) from product where deleted_flag = 0 and draft_flag = 0", Integer.class);
        if (count != null && count >= 8) {
            return;
        }

        Long sellerId = userId("seller01");
        if (sellerId == null) {
            return;
        }

        List<Map<String, Object>> batteries = jdbcTemplate.queryForList(
                "select br.id, br.battery_code as batteryCode, br.capacity_retention_rate as cap, br.internal_resistance_ratio as ir, " +
                        "br.cycle_count as cycleCount, ha.health_level as healthLevel " +
                        "from battery_record br left join health_assessment ha on ha.battery_id = br.id " +
                        "where br.created_by = 1 and br.is_deleted = 0 order by br.id asc"
        );
        if (batteries.isEmpty()) {
            return;
        }

        List<Object[]> templates = Arrays.asList(
                new Object[]{"磷酸铁锂 48V 退役模组 A 级", "适用于低速车与分布式储能示范项目，支持到站复检。", "深圳", "磷酸铁锂", 1980, 6, "https://dummyimage.com/900x620/dae7ec/164e63&text=LFP-A1"},
                new Object[]{"磷酸铁锂 51.2V 通讯储能包", "提供健康档案、循环数据与运输告知单。", "深圳", "磷酸铁锂", 2360, 5, "https://dummyimage.com/900x620/e2efe8/0d4b45&text=LFP-A2"},
                new Object[]{"三元锂 72V 动力模组检测件", "适合实验验证、拆解研究和教学演示。", "广州", "三元锂", 1680, 4, "https://dummyimage.com/900x620/f0e7df/7a4c1d&text=NCM-B1"},
                new Object[]{"储能梯次利用电池簇 10kWh", "附带近六个月健康趋势和建议应用场景。", "苏州", "磷酸铁锂", 5980, 3, "https://dummyimage.com/900x620/e3f4f1/125c57&text=ESS-C1"},
                new Object[]{"标准周转电池托盘套装", "适合仓储周转、备件池和项目试运行。", "上海", "磷酸铁锂", 1280, 8, "https://dummyimage.com/900x620/e9eef8/314d74&text=TRAY-D1"},
                new Object[]{"退役动力包实验样件", "用于容量筛查、BMS 验证和拆解教学。", "武汉", "三元锂", 980, 7, "https://dummyimage.com/900x620/f6ece4/8a5a2b&text=LAB-E1"},
                new Object[]{"工商业备用电源模组", "支持合同存证、模拟支付和发货流程演示。", "杭州", "磷酸铁锂", 3160, 2, "https://dummyimage.com/900x620/e2edf8/295989&text=BKP-F1"},
                new Object[]{"电池包维保替换模块", "适配售后场景，可快速查看物流追踪路径。", "南京", "磷酸铁锂", 2580, 5, "https://dummyimage.com/900x620/e5f3ec/22594a&text=SRV-G1"}
        );

        for (int i = 0; i < templates.size(); i++) {
            Object[] template = templates.get(i);
            Map<String, Object> battery = batteries.get(i % batteries.size());
            String title = String.valueOf(template[0]);
            Integer exists = jdbcTemplate.queryForObject("select count(1) from product where seller_id = ? and title = ? and deleted_flag = 0", Integer.class, sellerId, title);
            if (exists != null && exists > 0) {
                continue;
            }
            String cover = String.valueOf(template[6]);
            jdbcTemplate.update(
                    "insert into product(seller_id,battery_id,title,description,price,original_price,stock,cover_image,image_urls,shipping_from,shipping_type,battery_type,health_level,is_free_shipping,sale_count,view_count,favorite_count,publish_status,audit_status,draft_flag,deleted_flag,status) " +
                            "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    sellerId,
                    battery.get("id"),
                    title,
                    template[1],
                    template[4],
                    BigDecimal.valueOf(Double.parseDouble(String.valueOf(template[4])) + 320D),
                    template[5],
                    cover,
                    "[\"" + cover + "\"]",
                    template[2],
                    "物流",
                    template[3],
                    battery.get("healthLevel") == null ? "良好" : battery.get("healthLevel"),
                    true,
                    0,
                    10 + i * 3,
                    4 + i,
                    "ON_SHELF",
                    "APPROVED",
                    false,
                    false,
                    1
            );
        }
    }

    private double rounded(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private String resolveLife(String healthLevel) {
        if ("优秀".equals(healthLevel)) {
            return "3-5年";
        }
        if ("良好".equals(healthLevel)) {
            return "2-3年";
        }
        if ("一般".equals(healthLevel)) {
            return "1-2年";
        }
        if ("较差".equals(healthLevel)) {
            return "1年内";
        }
        return "建议停用";
    }

    private void upsertRole(String roleCode, String roleName) {
        Integer count = jdbcTemplate.queryForObject("select count(1) from role where role_code = ?", Integer.class, roleCode);
        if (count == null || count == 0) {
            jdbcTemplate.update("insert into role(role_code, role_name, description) values(?,?,?)", roleCode, roleName, roleName + "角色");
            return;
        }
        jdbcTemplate.update("update role set role_name=?, description=? where role_code=?", roleName, roleName + "角色", roleCode);
    }

    private void upsertUser(String username, String rawPassword, String realName, String email, String roleCode) {
        Long userId = jdbcTemplate.query(
                "select id from `user` where username = ?",
                rs -> rs.next() ? rs.getLong(1) : null,
                username
        );
        if (userId == null) {
            jdbcTemplate.update(
                    "insert into `user`(username,password,real_name,email,status) values(?,?,?,?,1)",
                    username, passwordEncoder.encode(rawPassword), realName, email
            );
            userId = jdbcTemplate.queryForObject("select id from `user` where username = ?", Long.class, username);
        } else {
            jdbcTemplate.update(
                    "update `user` set password=?, real_name=?, email=?, status=1 where id=?",
                    passwordEncoder.encode(rawPassword), realName, email, userId
            );
        }
        bindUserRole(userId, getRoleId(roleCode));
    }

    private void upsertProfile(String username, String nickname, String avatar, String bio, String city) {
        Long userId = userId(username);
        if (userId == null) {
            return;
        }
        Integer count = jdbcTemplate.queryForObject("select count(1) from user_profile where user_id = ?", Integer.class, userId);
        if (count == null || count == 0) {
            jdbcTemplate.update("insert into user_profile(user_id, nickname, avatar, bio, city) values(?,?,?,?,?)", userId, nickname, avatar, bio, city);
            return;
        }
        jdbcTemplate.update("update user_profile set nickname = ?, avatar = ?, bio = ?, city = ? where user_id = ?", nickname, avatar, bio, city, userId);
    }

    private void ensureAddress(String username, String receiverName, String receiverPhone, String province, String city, String district, String detailAddress, boolean isDefault) {
        Long userId = userId(username);
        if (userId == null) {
            return;
        }
        Integer count = jdbcTemplate.queryForObject("select count(1) from user_address where user_id = ?", Integer.class, userId);
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update(
                "insert into user_address(user_id, receiver_name, receiver_phone, province, city, district, detail_address, is_default) values(?,?,?,?,?,?,?,?)",
                userId, receiverName, receiverPhone, province, city, district, detailAddress, isDefault
        );
    }

    private void bindUserRole(Long userId, Long roleId) {
        Integer count = jdbcTemplate.queryForObject("select count(1) from user_role where user_id=? and role_id=?", Integer.class, userId, roleId);
        if (count == null || count == 0) {
            jdbcTemplate.update("insert into user_role(user_id, role_id) values(?,?)", userId, roleId);
        }
    }

    private Long getRoleId(String roleCode) {
        return jdbcTemplate.queryForObject("select id from role where role_code = ?", Long.class, roleCode);
    }

    private Long userId(String username) {
        return jdbcTemplate.query("select id from `user` where username = ?", rs -> rs.next() ? rs.getLong(1) : null, username);
    }

    private void upsertPermission(String code, String name, String path) {
        Integer count = jdbcTemplate.queryForObject("select count(1) from permission where perm_code=?", Integer.class, code);
        if (count == null || count == 0) {
            jdbcTemplate.update("insert into permission(perm_code,perm_name,perm_type,path) values(?,?,?,?)", code, name, "API", path);
            return;
        }
        jdbcTemplate.update("update permission set perm_name=?, perm_type='API', path=? where perm_code=?", name, path, code);
    }

    private static class BatterySeed {
        private final double cap;
        private final double ir;
        private final int cycle;
        private final double temp;

        private BatterySeed(double cap, double ir, int cycle, double temp) {
            this.cap = cap;
            this.ir = ir;
            this.cycle = cycle;
            this.temp = temp;
        }
    }
}
