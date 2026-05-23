package com.evbattery.modules.logistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.evbattery.common.util.PdfGenerator;
import com.evbattery.modules.logistics.entity.LogisticsInfo;
import com.evbattery.modules.logistics.mapper.LogisticsInfoMapper;
import com.evbattery.modules.logistics.service.LogisticsService;
import com.evbattery.modules.trade.entity.Order;
import com.evbattery.modules.trade.mapper.OrderMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LogisticsServiceImpl implements LogisticsService {

    @Resource
    private LogisticsInfoMapper logisticsInfoMapper;

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private ObjectMapper objectMapper;

    @Value("${app.storage.root:./storage}")
    private String storageRoot;

    @Override
    public Map<String, Object> saveTracking(Long orderId, String company, String trackingNo, String contactName, String contactPhone, Long currentUserId, boolean admin) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (!admin && !currentUserId.equals(order.getSellerId())) {
            throw new IllegalArgumentException("仅卖家可填写物流信息");
        }

        LogisticsInfo logisticsInfo = logisticsInfoMapper.selectOne(
                new LambdaQueryWrapper<LogisticsInfo>().eq(LogisticsInfo::getOrderId, orderId).last("limit 1")
        );
        if (logisticsInfo == null) {
            logisticsInfo = new LogisticsInfo();
            logisticsInfo.setOrderId(orderId);
        }

        logisticsInfo.setCompany(StringUtils.hasText(company) ? company : "平台模拟物流");
        logisticsInfo.setTrackingNo(StringUtils.hasText(trackingNo) ? trackingNo : ("MOCK" + System.currentTimeMillis()));
        logisticsInfo.setContactName(StringUtils.hasText(contactName) ? contactName : "平台应急联系人");
        logisticsInfo.setContactPhone(StringUtils.hasText(contactPhone) ? contactPhone : "400-800-1234");
        logisticsInfo.setHazardousNotice("本单涉及锂电池类货物，运输中应防短路、防挤压、防高温暴晒，并按危险品运输要求留存应急联系人。");
        logisticsInfo.setLastUpdatedAt(LocalDateTime.now());

        Map<String, Object> track = buildTracking(orderId, logisticsInfo.getCompany(), logisticsInfo.getTrackingNo(), logisticsInfo.getLastUpdatedAt());
        logisticsInfo.setStatus(String.valueOf(track.get("status")));
        logisticsInfo.setNodesJson(writeJson(track.get("nodes")));
        ensureNoticePdf(logisticsInfo, order);

        if (logisticsInfo.getId() == null) {
            logisticsInfoMapper.insert(logisticsInfo);
        } else {
            logisticsInfoMapper.updateById(logisticsInfo);
        }

        order.setOrderStatus("SHIPPED_PENDING_RECEIVE");
        order.setShipTime(LocalDateTime.now());
        order.setLogisticsCompany(logisticsInfo.getCompany());
        order.setLogisticsNo(logisticsInfo.getTrackingNo());
        orderMapper.updateById(order);
        return queryTracking(orderId, currentUserId, admin);
    }

    @Override
    public Map<String, Object> queryTracking(Long orderId, Long currentUserId, boolean admin) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (!admin && !currentUserId.equals(order.getBuyerId()) && !currentUserId.equals(order.getSellerId())) {
            throw new IllegalArgumentException("无权查看物流信息");
        }

        LogisticsInfo logisticsInfo = logisticsInfoMapper.selectOne(
                new LambdaQueryWrapper<LogisticsInfo>().eq(LogisticsInfo::getOrderId, orderId).last("limit 1")
        );

        if (logisticsInfo == null) {
            Map<String, Object> empty = new LinkedHashMap<String, Object>();
            empty.put("orderId", orderId);
            empty.put("status", "待发货");
            empty.put("company", order.getLogisticsCompany());
            empty.put("trackingNo", order.getLogisticsNo());
            empty.put("nodes", new ArrayList<Object>());
            empty.put("warning", false);
            empty.put("route", buildRouteInfo(order));
            return empty;
        }

        Map<String, Object> data = buildTracking(orderId, logisticsInfo.getCompany(), logisticsInfo.getTrackingNo(), logisticsInfo.getLastUpdatedAt());
        data.put("contactName", logisticsInfo.getContactName());
        data.put("contactPhone", logisticsInfo.getContactPhone());
        data.put("hazardousNotice", logisticsInfo.getHazardousNotice());
        data.put("noticeDownloadUrl", "/api/logistics/" + orderId + "/hazardous-notice");
        data.put("warning", logisticsInfo.getLastUpdatedAt() != null && Duration.between(logisticsInfo.getLastUpdatedAt(), LocalDateTime.now()).toHours() >= 48);
        data.put("route", buildRouteInfo(order));
        return data;
    }

    @Override
    public byte[] loadHazardousNoticePdf(Long orderId, Long currentUserId, boolean admin) {
        LogisticsInfo logisticsInfo = logisticsInfoMapper.selectOne(
                new LambdaQueryWrapper<LogisticsInfo>().eq(LogisticsInfo::getOrderId, orderId).last("limit 1")
        );
        Order order = orderMapper.selectById(orderId);
        if (order == null || logisticsInfo == null) {
            throw new IllegalArgumentException("物流信息不存在");
        }
        if (!admin && !currentUserId.equals(order.getBuyerId()) && !currentUserId.equals(order.getSellerId())) {
            throw new IllegalArgumentException("无权访问该告知单");
        }
        ensureNoticePdf(logisticsInfo, order);
        try {
            return Files.readAllBytes(Paths.get(logisticsInfo.getNoticePdfPath()));
        } catch (Exception ex) {
            throw new IllegalStateException("读取危险品运输告知单失败", ex);
        }
    }

    private void ensureNoticePdf(LogisticsInfo logisticsInfo, Order order) {
        Map<String, Object> product = jdbcTemplate.queryForMap(
                "select p.title, p.battery_type as batteryType from product p where p.id=?",
                order.getProductId()
        );
        List<String> lines = new ArrayList<String>();
        lines.add("订单编号：" + order.getOrderNo());
        lines.add("物流公司：" + logisticsInfo.getCompany());
        lines.add("运单号：" + logisticsInfo.getTrackingNo());
        lines.add("商品名称：" + product.get("title"));
        lines.add("电池类型：" + product.get("batteryType"));
        lines.add("托运提示：本货物为动力电池相关商品，包装、装卸、仓储与运输过程应满足锂电池货物安全运输要求。");
        lines.add("安全要求：1. 禁止与易燃、强氧化、强腐蚀物品混装；2. 防止正负极短路；3. 防止挤压、穿刺、跌落和剧烈冲击；4. 避免高温暴晒、雨淋和长时间靠近热源。");
        lines.add("应急处置：发现鼓包、泄漏、冒烟、异味或异常升温时，应立即隔离货物，远离火源，通知承运方和平台应急联系人处理。");
        lines.add("应急联系人：" + logisticsInfo.getContactName() + " / " + logisticsInfo.getContactPhone());
        lines.add("生成时间：" + LocalDateTime.now());
        byte[] pdfBytes = PdfGenerator.generateSimpleDocument("危险品运输告知单", lines);
        File pdfFile = Paths.get(storageRoot, "logistics", "notice-" + order.getId() + ".pdf").toFile();
        PdfGenerator.writeToFile(pdfFile, pdfBytes);
        logisticsInfo.setNoticePdfPath(pdfFile.getAbsolutePath());
    }

    private Map<String, Object> buildTracking(Long orderId, String company, String trackingNo, LocalDateTime lastUpdatedAt) {
        int selector = resolveProgressSelector(lastUpdatedAt);
        List<Map<String, Object>> nodes = new ArrayList<Map<String, Object>>();
        LocalDateTime base = lastUpdatedAt == null ? LocalDateTime.now().minusHours(12) : lastUpdatedAt.minusHours(12);
        nodes.add(node("已揽收", base, "货物已由承运方接收并完成入库扫描"));
        if (selector >= 1) {
            nodes.add(node("干线运输", base.plusHours(8), "运输车辆已发车，正在执行干线运输"));
        }
        if (selector >= 2) {
            nodes.add(node("到达分拨中心", base.plusHours(20), "货物已到达区域分拨中心，等待中转或末端派送"));
        }
        if (selector >= 3) {
            nodes.add(node("派送中", base.plusHours(28), "配送员已出站，预计今日送达"));
        }
        if (selector >= 4) {
            nodes.add(node("已签收", base.plusHours(32), "收货人已完成签收，运输流程结束"));
        }
        String status = String.valueOf(nodes.get(nodes.size() - 1).get("status"));
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("orderId", orderId);
        data.put("company", company);
        data.put("trackingNo", trackingNo);
        data.put("status", status);
        data.put("nodes", nodes);
        data.put("lastUpdatedAt", lastUpdatedAt);
        data.put("progressPercent", selector >= 4 ? 100 : Math.min(96, selector * 25 + 12));
        return data;
    }

    private int resolveProgressSelector(LocalDateTime lastUpdatedAt) {
        LocalDateTime reference = lastUpdatedAt == null ? LocalDateTime.now().minusHours(12) : lastUpdatedAt;
        long hours = Math.max(0, Duration.between(reference, LocalDateTime.now()).toHours());
        if (hours >= 20) return 4;
        if (hours >= 12) return 3;
        if (hours >= 6) return 2;
        if (hours >= 2) return 1;
        return 0;
    }

    private Map<String, Object> buildRouteInfo(Order order) {
        Map<String, Object> route = new LinkedHashMap<String, Object>();
        Map<String, Object> address = readAddress(order.getAddressSnapshot());
        String originCity = loadShippingFrom(order.getProductId());
        String destinationCity = firstNonEmpty(
                stringValue(address.get("city")),
                stringValue(address.get("province")),
                "上海"
        );
        route.put("origin", buildCityNode(originCity));
        route.put("destination", buildCityNode(destinationCity));
        route.put("checkpoints", buildCheckpoints(originCity, destinationCity));
        return route;
    }

    private Map<String, Object> readAddress(String json) {
        try {
            if (!StringUtils.hasText(json)) return new LinkedHashMap<String, Object>();
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            return new LinkedHashMap<String, Object>();
        }
    }

    private String loadShippingFrom(Long productId) {
        try {
            return jdbcTemplate.queryForObject("select ifnull(shipping_from, '上海') from product where id = ?", String.class, productId);
        } catch (Exception ex) {
            return "上海";
        }
    }

    private List<Map<String, Object>> buildCheckpoints(String originCity, String destinationCity) {
        List<Map<String, Object>> checkpoints = new ArrayList<Map<String, Object>>();
        for (String city : resolveRouteCities(originCity, destinationCity)) {
            Map<String, Object> node = buildCityNode(city);
            if (checkpoints.isEmpty() || !String.valueOf(checkpoints.get(checkpoints.size() - 1).get("city")).equals(node.get("city"))) {
                checkpoints.add(node);
            }
        }
        return checkpoints;
    }

    private List<String> resolveRouteCities(String originCity, String destinationCity) {
        String origin = normalizeCity(originCity);
        String destination = normalizeCity(destinationCity);
        List<String> cities = new ArrayList<String>();
        cities.add(origin);
        for (String city : routeTemplate(origin, destination)) {
            if (!cities.contains(city)) {
                cities.add(city);
            }
        }
        if (!cities.contains(destination)) {
            cities.add(destination);
        }
        return cities;
    }

    private List<String> routeTemplate(String origin, String destination) {
        if (isEast(origin) && isSouth(destination)) return Arrays.asList("苏州", "杭州", "南昌", "长沙", "广州");
        if (isSouth(origin) && isEast(destination)) return Arrays.asList("长沙", "南昌", "杭州", "苏州");
        if (isEast(origin) && isWest(destination)) return Arrays.asList("南京", "合肥", "武汉", "郑州", "西安", "成都");
        if (isWest(origin) && isEast(destination)) return Arrays.asList("成都", "西安", "郑州", "武汉", "合肥", "南京");
        if (isNorth(origin) && isSouth(destination)) return Arrays.asList("济南", "郑州", "武汉", "长沙", "广州");
        if (isSouth(origin) && isNorth(destination)) return Arrays.asList("长沙", "武汉", "郑州", "济南", "北京");
        if (isNorth(origin) && isWest(destination)) return Arrays.asList("石家庄", "郑州", "西安", "重庆");
        if (isWest(origin) && isSouth(destination)) return Arrays.asList("贵阳", "南宁", "广州", "深圳");
        if (origin.equals(destination)) return Arrays.asList(innerCityTransit(origin));
        if (isEast(origin) && isEast(destination)) return Arrays.asList("苏州", "南京", "合肥");
        if (isSouth(origin) && isSouth(destination)) return Arrays.asList("广州", "南宁", "长沙");
        if (isWest(origin) && isWest(destination)) return Arrays.asList("成都", "重庆", "贵阳");
        if (isNorth(origin) && isNorth(destination)) return Arrays.asList("济南", "天津", "北京");
        return Arrays.asList("南京", "合肥", "武汉");
    }

    private String innerCityTransit(String city) {
        if ("上海".equals(city)) return "苏州";
        if ("广州".equals(city)) return "佛山";
        if ("深圳".equals(city)) return "东莞";
        if ("北京".equals(city)) return "天津";
        return "南京";
    }

    private Map<String, Object> buildCityNode(String city) {
        Map<String, double[]> cityMap = cityCoordinateMap();
        String resolvedCity = normalizeCity(city);
        double[] lnglat = cityMap.getOrDefault(resolvedCity, cityMap.get("上海"));
        Map<String, Object> node = new LinkedHashMap<String, Object>();
        node.put("city", resolvedCity);
        node.put("lng", lnglat[0]);
        node.put("lat", lnglat[1]);
        return node;
    }

    private String normalizeCity(String city) {
        if (!StringUtils.hasText(city)) return "上海";
        String value = city.replace("市", "").replace("省", "").trim();
        List<String> supported = Arrays.asList(
                "上海", "北京", "深圳", "广州", "杭州", "苏州", "南京", "武汉", "西安", "成都", "重庆",
                "郑州", "长沙", "合肥", "天津", "青岛", "南昌", "济南", "石家庄", "佛山", "东莞",
                "厦门", "福州", "南宁", "贵阳", "昆明"
        );
        for (String item : supported) {
            if (value.contains(item)) return item;
        }
        return value;
    }

    private boolean isEast(String city) {
        return Arrays.asList("上海", "苏州", "杭州", "南京", "合肥", "青岛", "厦门", "福州", "南昌").contains(city);
    }

    private boolean isSouth(String city) {
        return Arrays.asList("广州", "深圳", "佛山", "东莞", "长沙", "南宁", "贵阳", "昆明", "厦门", "福州").contains(city);
    }

    private boolean isNorth(String city) {
        return Arrays.asList("北京", "天津", "济南", "石家庄", "青岛", "郑州").contains(city);
    }

    private boolean isWest(String city) {
        return Arrays.asList("西安", "成都", "重庆", "贵阳", "昆明").contains(city);
    }

    private Map<String, double[]> cityCoordinateMap() {
        Map<String, double[]> map = new HashMap<String, double[]>();
        map.put("上海", new double[]{121.4737D, 31.2304D});
        map.put("北京", new double[]{116.4074D, 39.9042D});
        map.put("深圳", new double[]{114.0579D, 22.5431D});
        map.put("广州", new double[]{113.2644D, 23.1291D});
        map.put("杭州", new double[]{120.1551D, 30.2741D});
        map.put("苏州", new double[]{120.5853D, 31.2989D});
        map.put("南京", new double[]{118.7969D, 32.0603D});
        map.put("武汉", new double[]{114.3054D, 30.5931D});
        map.put("西安", new double[]{108.9398D, 34.3416D});
        map.put("成都", new double[]{104.0665D, 30.5723D});
        map.put("重庆", new double[]{106.5516D, 29.5630D});
        map.put("郑州", new double[]{113.6254D, 34.7466D});
        map.put("长沙", new double[]{112.9388D, 28.2282D});
        map.put("合肥", new double[]{117.2272D, 31.8206D});
        map.put("天津", new double[]{117.2000D, 39.1333D});
        map.put("青岛", new double[]{120.3826D, 36.0671D});
        map.put("南昌", new double[]{115.8579D, 28.6820D});
        map.put("济南", new double[]{117.1201D, 36.6512D});
        map.put("石家庄", new double[]{114.5149D, 38.0428D});
        map.put("佛山", new double[]{113.1214D, 23.0215D});
        map.put("东莞", new double[]{113.7518D, 23.0207D});
        map.put("厦门", new double[]{118.0894D, 24.4798D});
        map.put("福州", new double[]{119.2965D, 26.0745D});
        map.put("南宁", new double[]{108.3669D, 22.8170D});
        map.put("贵阳", new double[]{106.6302D, 26.6470D});
        map.put("昆明", new double[]{102.8329D, 24.8801D});
        return map;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String firstNonEmpty(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) return value;
        }
        return "";
    }

    private Map<String, Object> node(String status, LocalDateTime time, String description) {
        Map<String, Object> node = new LinkedHashMap<String, Object>();
        node.put("status", status);
        node.put("time", time);
        node.put("description", description);
        return node;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "[]";
        }
    }
}
