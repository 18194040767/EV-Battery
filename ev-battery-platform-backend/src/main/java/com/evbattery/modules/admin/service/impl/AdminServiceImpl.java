package com.evbattery.modules.admin.service.impl;

import com.evbattery.modules.admin.service.AdminService;
import com.evbattery.modules.statistics.service.StatisticsService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class AdminServiceImpl implements AdminService {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private StatisticsService statisticsService;

    @Override
    public boolean isAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        Integer count = jdbcTemplate.queryForObject(
                "select count(1) from user_role ur join role r on ur.role_id=r.id where ur.user_id=? and r.role_code='ROLE_ADMIN'",
                Integer.class,
                userId
        );
        return count != null && count > 0;
    }

    @Override
    public Map<String, Object> dashboard() {
        return statisticsService.adminOverview();
    }

    @Override
    public List<Map<String, Object>> users() {
        return jdbcTemplate.queryForList(
                "select u.id, u.username, u.real_name as realName, u.phone, u.email, u.status, " +
                        "group_concat(r.role_code order by r.role_code separator ',') as roles, u.created_at as createdAt " +
                        "from `user` u left join user_role ur on u.id = ur.user_id left join role r on ur.role_id = r.id " +
                        "group by u.id order by u.id desc"
        );
    }

    @Override
    @Transactional
    public void updateUser(Map<String, Object> payload) {
        Long id = Long.parseLong(String.valueOf(payload.get("id")));
        if (payload.get("status") != null) {
            jdbcTemplate.update("update `user` set status=? where id=?", payload.get("status"), id);
        }
        if (payload.get("email") != null) {
            jdbcTemplate.update("update `user` set email=? where id=?", payload.get("email"), id);
        }
        if (payload.get("realName") != null) {
            jdbcTemplate.update("update `user` set real_name=? where id=?", payload.get("realName"), id);
        }
        if (payload.get("roleCode") != null) {
            String roleCode = String.valueOf(payload.get("roleCode"));
            jdbcTemplate.update("delete from user_role where user_id=?", id);
            Long roleId = jdbcTemplate.queryForObject("select id from role where role_code=?", Long.class, roleCode);
            jdbcTemplate.update("insert into user_role(user_id, role_id) values(?,?)", id, roleId);
        }
    }

    @Override
    public void resetPassword(Long userId) {
        jdbcTemplate.update("update `user` set password=? where id=?", passwordEncoder.encode("123456"), userId);
    }

    @Override
    public List<Map<String, Object>> batteries() {
        return jdbcTemplate.queryForList(
                "select b.id, b.battery_code as batteryCode, b.source_type as sourceType, b.audit_status as auditStatus, b.status, b.remark, " +
                        "b.capacity_retention_rate as capacityRetentionRate, b.cycle_count as cycleCount, b.created_at as createdAt, u.username as ownerName " +
                        "from battery_record b left join `user` u on b.created_by = u.id order by b.id desc"
        );
    }

    @Override
    public void auditBattery(Long batteryId, Integer auditStatus, String remark) {
        jdbcTemplate.update("update battery_record set audit_status=?, remark=? where id=?", auditStatus, remark, batteryId);
    }

    @Override
    public List<Map<String, Object>> products() {
        return jdbcTemplate.queryForList(
                "select p.id, p.title, p.price, p.stock, p.publish_status as publishStatus, p.audit_status as auditStatus, " +
                        "p.battery_type as batteryType, p.health_level as healthLevel, p.created_at as createdAt, u.username as sellerName " +
                        "from product p left join `user` u on p.seller_id = u.id where p.deleted_flag = 0 order by p.id desc"
        );
    }

    @Override
    public void auditProduct(Long productId, String auditStatus, String publishStatus) {
        jdbcTemplate.update("update product set audit_status=?, publish_status=? where id=?", auditStatus, publishStatus, productId);
    }

    @Override
    public List<Map<String, Object>> orders() {
        return jdbcTemplate.queryForList(
                "select o.id, o.order_no as orderNo, o.amount, o.quantity, o.order_status as orderStatus, o.pay_status as payStatus, " +
                        "o.logistics_company as logisticsCompany, o.logistics_no as logisticsNo, o.created_at as createdAt, " +
                        "buyer.username as buyerName, seller.username as sellerName, p.title " +
                        "from `order` o left join `user` buyer on o.buyer_id = buyer.id left join `user` seller on o.seller_id = seller.id " +
                        "left join product p on o.product_id = p.id order by o.id desc"
        );
    }

    @Override
    public void forceCancelOrder(Long orderId) {
        jdbcTemplate.update("update `order` set order_status='CANCELLED', cancel_time=? where id=?", LocalDateTime.now(), orderId);
    }

    @Override
    public void markOrderShipped(Long orderId, String company, String trackingNo) {
        jdbcTemplate.update(
                "update `order` set order_status='SHIPPED_PENDING_RECEIVE', ship_time=?, logistics_company=?, logistics_no=? where id=?",
                LocalDateTime.now(),
                StringUtils.hasText(company) ? company : "顺丰速运",
                StringUtils.hasText(trackingNo) ? trackingNo : ("MOCK" + System.currentTimeMillis()),
                orderId
        );
    }

    @Override
    public List<Map<String, Object>> contracts() {
        return jdbcTemplate.queryForList(
                "select c.id, c.contract_no as contractNo, c.order_id as orderId, c.hash_digest as hashDigest, c.content_hash as contentHash, c.pdf_hash as pdfHash, " +
                        "c.verify_count as verifyCount, c.created_at as createdAt, o.order_no as orderNo, buyer.username as buyerName, seller.username as sellerName " +
                        "from contract c left join `order` o on c.order_id = o.id left join `user` buyer on o.buyer_id = buyer.id left join `user` seller on o.seller_id = seller.id order by c.id desc"
        );
    }
}
