package com.evbattery.modules.statistics.service.impl;

import com.evbattery.modules.statistics.service.StatisticsService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Override
    public Map<String, Object> tradeTrend(Integer days) {
        int length = days == null || days <= 0 ? 7 : days;
        List<String> xAxis = new ArrayList<String>();
        List<Object> amountSeries = new ArrayList<Object>();
        List<Object> orderSeries = new ArrayList<Object>();
        for (int i = length - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            xAxis.add(date.toString());
            Double amount = jdbcTemplate.queryForObject(
                    "select ifnull(sum(amount), 0) from `order` where date(created_at)=?",
                    Double.class,
                    date.toString()
            );
            Integer count = jdbcTemplate.queryForObject(
                    "select count(1) from `order` where date(created_at)=?",
                    Integer.class,
                    date.toString()
            );
            amountSeries.add(amount == null ? 0 : amount);
            orderSeries.add(count == null ? 0 : count);
        }
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("xAxis", xAxis);
        data.put("amountSeries", amountSeries);
        data.put("orderSeries", orderSeries);
        return data;
    }

    @Override
    public List<Map<String, Object>> healthDistribution(Integer days) {
        int length = normalizeDays(days, 0);
        if (length > 0) {
            return jdbcTemplate.queryForList(
                    "select ifnull(health_level, 'UNKNOWN') as name, count(1) as value from health_assessment where ifnull(assessment_time, created_at) >= date_sub(now(), interval ? day) group by health_level order by value desc",
                    length
            );
        }
        return jdbcTemplate.queryForList("select ifnull(health_level, 'UNKNOWN') as name, count(1) as value from health_assessment group by health_level order by value desc");
    }

    @Override
    public List<Map<String, Object>> sourceDistribution() {
        return jdbcTemplate.queryForList("select ifnull(source_type, 'UNKNOWN') as name, count(1) as value from battery_record group by source_type order by value desc");
    }

    @Override
    public List<Map<String, Object>> productCategoryDistribution(Integer days) {
        int length = normalizeDays(days, 0);
        if (length > 0) {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "select ifnull(nullif(trim(battery_type), ''), 'UNKNOWN') as name, count(1) as value from product where deleted_flag = 0 and created_at >= date_sub(now(), interval ? day) group by ifnull(nullif(trim(battery_type), ''), 'UNKNOWN') order by value desc",
                    length
            );
            if (!rows.isEmpty()) {
                return rows;
            }
        }
        return jdbcTemplate.queryForList("select ifnull(nullif(trim(battery_type), ''), 'UNKNOWN') as name, count(1) as value from product where deleted_flag = 0 group by ifnull(nullif(trim(battery_type), ''), 'UNKNOWN') order by value desc");
    }

    @Override
    public Map<String, Object> adminOverview() {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        Integer totalUsers = jdbcTemplate.queryForObject("select count(1) from `user`", Integer.class);
        Integer totalProducts = jdbcTemplate.queryForObject("select count(1) from product where deleted_flag = 0", Integer.class);
        Integer totalOrders = jdbcTemplate.queryForObject("select count(1) from `order`", Integer.class);
        Double totalAmount30d = jdbcTemplate.queryForObject("select ifnull(sum(amount), 0) from `order` where created_at >= date_sub(now(), interval 30 day)", Double.class);
        Map<String, Object> metrics = new LinkedHashMap<String, Object>();
        metrics.put("totalUsers", totalUsers == null ? 0 : totalUsers);
        metrics.put("totalProducts", totalProducts == null ? 0 : totalProducts);
        metrics.put("totalOrders", totalOrders == null ? 0 : totalOrders);
        metrics.put("totalAmount30d", totalAmount30d == null ? 0 : totalAmount30d);
        data.put("metrics", metrics);
        data.put("tradeTrend7d", tradeTrend(7));
        data.put("tradeTrend30d", tradeTrend(30));
        data.put("healthDistribution", healthDistribution(30));
        data.put("productCategoryDistribution", productCategoryDistribution(30));
        data.put("recentOrders", jdbcTemplate.queryForList(
                "select o.id, o.order_no as orderNo, o.amount, o.order_status as orderStatus, o.created_at as createdAt, p.title, up.nickname as buyerName " +
                        "from `order` o left join product p on o.product_id = p.id left join user_profile up on o.buyer_id = up.user_id order by o.id desc limit 8"
        ));
        data.put("provinceDistribution", jdbcTemplate.queryForList(
                "select ifnull(province, '未知地区') as name, count(1) as value from user_address group by province order by value desc limit 10"
        ));
        return data;
    }

    private int normalizeDays(Integer days, int fallback) {
        if (days == null) {
            return fallback;
        }
        if (days <= 0) {
            return fallback;
        }
        return Math.min(days, 365);
    }
}
