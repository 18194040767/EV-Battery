
package com.evbattery.modules.trade.controller;

import com.evbattery.common.AuthUserContext;
import com.evbattery.common.result.Result;
import com.evbattery.modules.assessment.entity.HealthAssessment;
import com.evbattery.modules.assessment.mapper.HealthAssessmentMapper;
import com.evbattery.modules.contract.service.ContractService;
import com.evbattery.modules.logistics.service.LogisticsService;
import com.evbattery.modules.trade.entity.Order;
import com.evbattery.modules.trade.entity.Product;
import com.evbattery.modules.trade.mapper.OrderMapper;
import com.evbattery.modules.trade.mapper.ProductMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/trade")
public class TradeController {

    @Resource
    private ProductMapper productMapper;
    @Resource
    private OrderMapper orderMapper;
    @Resource
    private HealthAssessmentMapper healthAssessmentMapper;
    @Resource
    private JdbcTemplate jdbcTemplate;
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private ContractService contractService;
    @Resource
    private LogisticsService logisticsService;

    @GetMapping("/products")
    public Result<Map<String, Object>> products(@RequestParam(defaultValue = "1") Integer page,
                                                @RequestParam(defaultValue = "12") Integer size,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) BigDecimal minPrice,
                                                @RequestParam(required = false) BigDecimal maxPrice,
                                                @RequestParam(required = false) String healthLevel,
                                                @RequestParam(required = false) String batteryType,
                                                @RequestParam(required = false) String shippingFrom,
                                                @RequestParam(required = false) String sortBy) {
        List<Object> args = new ArrayList<Object>();
        StringBuilder where = new StringBuilder(" where p.deleted_flag = 0 and p.draft_flag = 0 and p.publish_status in ('ON_SHELF','SOLD_OUT') ");
        if (StringUtils.hasText(keyword)) {
            where.append(" and (p.title like ? or b.battery_code like ? or p.description like ?)");
            String fuzzy = "%" + keyword.trim() + "%";
            args.add(fuzzy);
            args.add(fuzzy);
            args.add(fuzzy);
        }
        if (minPrice != null) { where.append(" and p.price >= ?"); args.add(minPrice); }
        if (maxPrice != null) { where.append(" and p.price <= ?"); args.add(maxPrice); }
        if (StringUtils.hasText(healthLevel)) { where.append(" and p.health_level = ?"); args.add(healthLevel); }
        if (StringUtils.hasText(batteryType)) { where.append(" and p.battery_type = ?"); args.add(batteryType); }
        if (StringUtils.hasText(shippingFrom)) { where.append(" and p.shipping_from like ?"); args.add("%" + shippingFrom.trim() + "%"); }
        String baseFrom = " from product p left join battery_record b on p.battery_id = b.id left join user_profile up on p.seller_id = up.user_id left join credit_score cs on p.seller_id = cs.user_id ";
        Integer total = jdbcTemplate.queryForObject("select count(1)" + baseFrom + where, Integer.class, args.toArray());
        String orderBy = resolveProductSort(sortBy);
        List<Object> queryArgs = new ArrayList<Object>(args);
        queryArgs.add((page - 1) * size);
        queryArgs.add(size);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select p.*, b.battery_code as batteryCode, up.nickname as sellerNickname, up.avatar as sellerAvatar, ifnull(cs.score,100) as creditScore " +
                        baseFrom + where + " order by " + orderBy + " limit ?, ?",
                queryArgs.toArray());
        for (Map<String, Object> row : rows) row.put("images", parseJsonArray(String.valueOf(row.get("image_urls"))));
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("records", rows);
        data.put("total", total == null ? 0 : total);
        return Result.success(data);
    }

    @GetMapping("/products/{id}")
    public Result<Map<String, Object>> productDetail(@PathVariable Long id) {
        jdbcTemplate.update("update product set view_count = ifnull(view_count, 0) + 1 where id = ?", id);
        Map<String, Object> product = singleMap(
                "select p.*, b.battery_code as batteryCode, b.capacity_retention_rate as capacityRetentionRate, b.internal_resistance_ratio as internalResistanceRatio, b.cycle_count as cycleCount, b.avg_temperature as avgTemperature, up.nickname as sellerNickname, up.avatar as sellerAvatar, up.bio as sellerBio, ifnull(cs.score,100) as creditScore from product p left join battery_record b on p.battery_id = b.id left join user_profile up on p.seller_id = up.user_id left join credit_score cs on p.seller_id = cs.user_id where p.id = ?",
                id);
        if (product == null) return Result.fail(404, "商品不存在");
        product.put("images", parseJsonArray(String.valueOf(product.get("image_urls"))));
        product.put("favorite", isFavorite(currentUserId(), id));
        product.put("latestAssessment", latestAssessment(Long.parseLong(String.valueOf(product.get("battery_id")))));
        product.put("reviews", jdbcTemplate.queryForList("select r.*, up.nickname as reviewerName from product_review r left join user_profile up on r.reviewer_id = up.user_id where r.product_id = ? order by r.id desc limit 5", id));
        product.put("recommendations", jdbcTemplate.queryForList("select id, title, price, cover_image as coverImage, health_level as healthLevel from product where id <> ? and deleted_flag = 0 and draft_flag = 0 and (health_level = ? or battery_type = ?) order by id desc limit 4", id, product.get("health_level"), product.get("battery_type")));
        return Result.success(product);
    }

    @GetMapping("/products/mine")
    public Result<List<Map<String, Object>>> myProducts() {
        return Result.success(jdbcTemplate.queryForList("select * from product where seller_id = ? and deleted_flag = 0 order by id desc", currentUserId()));
    }

    @PostMapping("/products")
    public Result<Map<String, Object>> publish(@RequestBody Map<String, Object> body) {
        Product product = toProduct(body);
        product.setSellerId(currentUserId());
        product.setDraftFlag(Boolean.TRUE.equals(body.get("draftFlag")));
        product.setDeletedFlag(Boolean.FALSE);
        if (!StringUtils.hasText(product.getPublishStatus())) product.setPublishStatus(product.getDraftFlag() ? "DRAFT" : "ON_SHELF");
        if (!StringUtils.hasText(product.getAuditStatus())) product.setAuditStatus("APPROVED");
        if (product.getStatus() == null) product.setStatus(1);
        if (product.getIsFreeShipping() == null) product.setIsFreeShipping(Boolean.TRUE);
        if (product.getStock() == null) product.setStock(1);
        if (product.getBatteryId() == null) return Result.fail(400, "请选择电池档案");
        fillBatteryDerivedFields(product);
        productMapper.insert(product);
        return Result.success("发布成功", productDetailMap(product.getId()));
    }

    @PutMapping("/products/{id}")
    public Result<Map<String, Object>> updateProduct(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Product product = productMapper.selectById(id);
        if (product == null || !currentUserId().equals(product.getSellerId())) return Result.fail(404, "商品不存在");
        Product incoming = toProduct(body);
        if (incoming.getBatteryId() != null) product.setBatteryId(incoming.getBatteryId());
        if (StringUtils.hasText(incoming.getTitle())) product.setTitle(incoming.getTitle());
        if (incoming.getDescription() != null) product.setDescription(incoming.getDescription());
        if (incoming.getPrice() != null) product.setPrice(incoming.getPrice());
        if (incoming.getOriginalPrice() != null) product.setOriginalPrice(incoming.getOriginalPrice());
        if (incoming.getStock() != null) product.setStock(incoming.getStock());
        if (incoming.getCoverImage() != null) product.setCoverImage(incoming.getCoverImage());
        if (incoming.getImageUrls() != null) product.setImageUrls(incoming.getImageUrls());
        if (incoming.getShippingFrom() != null) product.setShippingFrom(incoming.getShippingFrom());
        if (incoming.getShippingType() != null) product.setShippingType(incoming.getShippingType());
        if (incoming.getBatteryType() != null) product.setBatteryType(incoming.getBatteryType());
        if (incoming.getPublishStatus() != null) product.setPublishStatus(incoming.getPublishStatus());
        if (incoming.getIsFreeShipping() != null) product.setIsFreeShipping(incoming.getIsFreeShipping());
        fillBatteryDerivedFields(product);
        productMapper.updateById(product);
        return Result.success("更新成功", productDetailMap(id));
    }

    @PatchMapping("/products/{id}/status")
    public Result<String> changeProductStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Product product = productMapper.selectById(id);
        if (product == null || !currentUserId().equals(product.getSellerId())) return Result.fail(404, "商品不存在");
        product.setPublishStatus(String.valueOf(body.getOrDefault("publishStatus", "ON_SHELF")));
        productMapper.updateById(product);
        return Result.success("状态已更新", null);
    }

    @PostMapping("/products/draft")
    public Result<Map<String, Object>> saveDraft(@RequestBody Map<String, Object> body) {
        Long id = body.get("id") == null ? null : Long.parseLong(String.valueOf(body.get("id")));
        String draftData = writeJson(body.get("draftData"));
        if (id == null) {
            jdbcTemplate.update("insert into product_draft(seller_id, title, draft_data) values(?,?,?)", currentUserId(), String.valueOf(body.getOrDefault("title", "商品草稿")), draftData);
            id = jdbcTemplate.queryForObject("select max(id) from product_draft where seller_id = ?", Long.class, currentUserId());
        } else {
            jdbcTemplate.update("update product_draft set title = ?, draft_data = ? where id = ? and seller_id = ?", String.valueOf(body.getOrDefault("title", "商品草稿")), draftData, id, currentUserId());
        }
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("id", id);
        return Result.success("草稿已保存", data);
    }

    @GetMapping("/products/draft/list")
    public Result<List<Map<String, Object>>> draftList() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("select * from product_draft where seller_id = ? order by id desc", currentUserId());
        for (Map<String, Object> row : rows) row.put("draftData", parseJsonObject(String.valueOf(row.get("draft_data"))));
        return Result.success(rows);
    }

    @DeleteMapping("/products/draft/{id}")
    public Result<String> deleteDraft(@PathVariable Long id) {
        jdbcTemplate.update("delete from product_draft where id = ? and seller_id = ?", id, currentUserId());
        return Result.success("草稿已删除", null);
    }

    @PostMapping("/favorites/{productId}")
    public Result<String> addFavorite(@PathVariable Long productId) {
        if (!isFavorite(currentUserId(), productId)) {
            jdbcTemplate.update("insert into favorite_product(user_id, product_id) values(?,?)", currentUserId(), productId);
            jdbcTemplate.update("update product set favorite_count = ifnull(favorite_count, 0) + 1 where id = ?", productId);
        }
        return Result.success("已收藏", null);
    }

    @DeleteMapping("/favorites/{productId}")
    public Result<String> removeFavorite(@PathVariable Long productId) {
        jdbcTemplate.update("delete from favorite_product where user_id = ? and product_id = ?", currentUserId(), productId);
        jdbcTemplate.update("update product set favorite_count = greatest(0, ifnull(favorite_count, 0) - 1) where id = ?", productId);
        return Result.success("已取消收藏", null);
    }

    @GetMapping("/favorites")
    public Result<List<Map<String, Object>>> favoriteList() {
        return Result.success(jdbcTemplate.queryForList("select p.*, f.created_at as favoriteTime from favorite_product f join product p on f.product_id = p.id where f.user_id = ? and p.deleted_flag = 0 order by f.id desc", currentUserId()));
    }

    @GetMapping("/favorites/{productId}/status")
    public Result<Map<String, Object>> favoriteStatus(@PathVariable Long productId) {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("favorite", isFavorite(currentUserId(), productId));
        return Result.success(data);
    }

    @PostMapping("/demand/publish")
    public Result<String> publishDemand(@RequestBody Map<String, Object> body) {
        jdbcTemplate.update("insert into purchase_demand(buyer_id, title, requirement, budget_min, budget_max, status) values(?,?,?,?,?,1)",
                currentUserId(), body.get("title"), body.get("requirement"), body.get("budgetMin"), body.get("budgetMax"));
        return Result.success("需求已发布", null);
    }

    @GetMapping("/demand/list")
    public Result<List<Map<String, Object>>> demandList() {
        return Result.success(jdbcTemplate.queryForList("select * from purchase_demand order by id desc"));
    }

    @GetMapping("/cart")
    public Result<List<Map<String, Object>>> cartList() {
        return Result.success(jdbcTemplate.queryForList(
                "select c.id as cartId, c.product_id as productId, c.quantity, c.checked_flag as checkedFlag, " +
                        "p.id as id, p.*, if(p.stock < c.quantity or p.publish_status <> 'ON_SHELF', 1, 0) as invalidFlag " +
                        "from cart_item c join product p on c.product_id = p.id where c.user_id = ? order by c.id desc",
                currentUserId()
        ));
    }

    @PostMapping("/cart")
    public Result<String> addCart(@RequestBody Map<String, Object> body) {
        Long productId = Long.parseLong(String.valueOf(body.get("productId")));
        Integer quantity = Integer.parseInt(String.valueOf(body.getOrDefault("quantity", 1)));
        Product product = productMapper.selectById(productId);
        if (product == null || Boolean.TRUE.equals(product.getDeletedFlag()) || !"ON_SHELF".equals(product.getPublishStatus())) {
            return Result.fail(404, "商品不可加入购物车");
        }
        if (currentUserId().equals(product.getSellerId())) {
            return Result.fail(400, "不能购买自己发布的商品");
        }
        if (product.getStock() == null || product.getStock() < quantity) {
            return Result.fail(400, "库存不足");
        }
        Integer count = jdbcTemplate.queryForObject("select count(1) from cart_item where user_id = ? and product_id = ?", Integer.class, currentUserId(), productId);
        if (count != null && count > 0) {
            Integer existingQuantity = jdbcTemplate.queryForObject("select quantity from cart_item where user_id = ? and product_id = ?", Integer.class, currentUserId(), productId);
            int nextQuantity = (existingQuantity == null ? 0 : existingQuantity) + quantity;
            if (product.getStock() < nextQuantity) {
                return Result.fail(400, "加入数量超过当前库存");
            }
            jdbcTemplate.update("update cart_item set quantity = ? where user_id = ? and product_id = ?", nextQuantity, currentUserId(), productId);
        } else {
            jdbcTemplate.update("insert into cart_item(user_id, product_id, quantity, checked_flag) values(?,?,?,1)", currentUserId(), productId, quantity);
        }
        return Result.success("已加入购物车", null);
    }

    @PutMapping("/cart/{id}")
    public Result<String> updateCart(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        jdbcTemplate.update("update cart_item set quantity = ?, checked_flag = ? where id = ? and user_id = ?",
                Integer.parseInt(String.valueOf(body.getOrDefault("quantity", 1))),
                Boolean.parseBoolean(String.valueOf(body.getOrDefault("checkedFlag", true))), id, currentUserId());
        return Result.success("购物车已更新", null);
    }

    @DeleteMapping("/cart/{id}")
    public Result<String> deleteCart(@PathVariable Long id) {
        jdbcTemplate.update("delete from cart_item where id = ? and user_id = ?", id, currentUserId());
        return Result.success("已删除", null);
    }

    @DeleteMapping("/cart")
    public Result<String> clearInvalidCart() {
        jdbcTemplate.update("delete c from cart_item c join product p on c.product_id = p.id where c.user_id = ? and (p.publish_status <> 'ON_SHELF' or p.stock < c.quantity)", currentUserId());
        return Result.success("已清理失效商品", null);
    }

    @GetMapping("/addresses")
    public Result<List<Map<String, Object>>> addresses() {
        return Result.success(jdbcTemplate.queryForList("select * from user_address where user_id = ? order by is_default desc, id desc", currentUserId()));
    }

    @PostMapping("/addresses")
    public Result<String> saveAddress(@RequestBody Map<String, Object> body) {
        Long id = body.get("id") == null ? null : Long.parseLong(String.valueOf(body.get("id")));
        boolean isDefault = Boolean.parseBoolean(String.valueOf(body.getOrDefault("isDefault", true)));
        if (isDefault) jdbcTemplate.update("update user_address set is_default = 0 where user_id = ?", currentUserId());
        if (id == null) {
            jdbcTemplate.update("insert into user_address(user_id, receiver_name, receiver_phone, province, city, district, detail_address, is_default) values(?,?,?,?,?,?,?,?)",
                    currentUserId(), body.get("receiverName"), body.get("receiverPhone"), body.get("province"), body.get("city"), body.get("district"), body.get("detailAddress"), isDefault);
        } else {
            jdbcTemplate.update("update user_address set receiver_name=?, receiver_phone=?, province=?, city=?, district=?, detail_address=?, is_default=? where id=? and user_id=?",
                    body.get("receiverName"), body.get("receiverPhone"), body.get("province"), body.get("city"), body.get("district"), body.get("detailAddress"), isDefault, id, currentUserId());
        }
        return Result.success("地址已保存", null);
    }

    @PostMapping("/orders/confirm")
    public Result<Map<String, Object>> createOrder(@RequestBody Map<String, Object> body) {
        List<Map<String, Object>> items = body.get("items") == null ? new ArrayList<Map<String, Object>>() : objectMapper.convertValue(body.get("items"), new TypeReference<List<Map<String, Object>>>() {});
        if (items.isEmpty() && body.get("productId") != null) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("productId", body.get("productId"));
            item.put("quantity", body.getOrDefault("quantity", 1));
            items.add(item);
        }
        if (items.isEmpty()) return Result.fail(400, "请选择商品");
        Long addressId = body.get("addressId") == null ? null : Long.parseLong(String.valueOf(body.get("addressId")));
        Map<String, Object> address = addressId == null ? defaultAddress() : singleMap("select * from user_address where id = ? and user_id = ?", addressId, currentUserId());
        if (address == null) return Result.fail(400, "请先填写收货地址");
        List<Map<String, Object>> created = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> item : items) {
            Long productId = Long.parseLong(String.valueOf(item.get("productId")));
            Integer quantity = Integer.parseInt(String.valueOf(item.getOrDefault("quantity", 1)));
            Product product = productMapper.selectById(productId);
            if (product == null || Boolean.TRUE.equals(product.getDeletedFlag())) return Result.fail(404, "商品不存在");
            if (currentUserId().equals(product.getSellerId())) return Result.fail(400, "不能下单自己发布的商品");
            if (!"ON_SHELF".equals(product.getPublishStatus())) return Result.fail(400, "商品当前不可下单");
            if (product.getStock() == null || product.getStock() < quantity) return Result.fail(400, "库存不足");
            Order order = new Order();
            order.setOrderNo("OD" + System.currentTimeMillis() + productId);
            order.setProductId(productId);
            order.setBuyerId(currentUserId());
            order.setSellerId(product.getSellerId());
            order.setQuantity(quantity);
            order.setUnitPrice(product.getPrice());
            order.setAmount(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
            order.setAddressSnapshot(writeJson(address));
            order.setProductSnapshot(writeJson(productDetailMap(productId)));
            order.setOrderStatus("PENDING_PAYMENT");
            order.setPayStatus("UNPAID");
            order.setPaymentMethod(String.valueOf(body.getOrDefault("paymentMethod", "MOCK_WECHAT")));
            orderMapper.insert(order);
            created.add(orderDetailMap(order.getId()));
            jdbcTemplate.update("delete from cart_item where user_id = ? and product_id = ?", currentUserId(), productId);
        }
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("orders", created);
        return Result.success("订单已创建", data);
    }

    @GetMapping("/orders")
    public Result<List<Map<String, Object>>> orders(@RequestParam(required = false) String tab) {
        Long uid = currentUserId();
        StringBuilder sql = new StringBuilder("select * from `order` where (buyer_id = ? or seller_id = ?) ");
        List<Object> args = new ArrayList<Object>();
        args.add(uid);
        args.add(uid);
        if (StringUtils.hasText(tab) && !"ALL".equalsIgnoreCase(tab)) { sql.append(" and order_status = ?"); args.add(tab); }
        sql.append(" order by id desc");
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), args.toArray());
        for (Map<String, Object> row : rows) {
            row.put("addressSnapshot", parseJsonObject(String.valueOf(row.get("address_snapshot"))));
            row.put("productSnapshot", parseJsonObject(String.valueOf(row.get("product_snapshot"))));
            row.put("review", singleMap("select score, content, created_at as createdAt from product_review where order_id = ?", row.get("id")));
        }
        return Result.success(rows);
    }

    @GetMapping("/orders/{id}")
    public Result<Map<String, Object>> orderDetail(@PathVariable Long id) {
        Map<String, Object> data = orderDetailMap(id);
        if (data == null) return Result.fail(404, "订单不存在");
        return Result.success(data);
    }

    @PutMapping("/orders/{id}/address")
    public Result<Map<String, Object>> updateOrderAddress(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Order order = orderMapper.selectById(id);
        if (order == null || !currentUserId().equals(order.getBuyerId())) return Result.fail(404, "订单不存在");
        if (!"PENDING_PAYMENT".equals(order.getOrderStatus())) return Result.fail(400, "当前状态不可修改地址");
        Long addressId = body.get("addressId") == null ? null : Long.parseLong(String.valueOf(body.get("addressId")));
        Map<String, Object> address = addressId == null ? defaultAddress() : singleMap("select * from user_address where id = ? and user_id = ?", addressId, currentUserId());
        if (address == null) return Result.fail(400, "收货地址不存在");
        order.setAddressSnapshot(writeJson(address));
        orderMapper.updateById(order);
        return Result.success("收货地址已更新", orderDetailMap(id));
    }

    @PostMapping("/orders/{id}/pay")
    public Result<String> pay(@PathVariable Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null || !currentUserId().equals(order.getBuyerId())) return Result.fail(404, "订单不存在");
        if ("PAID".equals(order.getPayStatus())) return Result.success("支付成功", null);
        Product product = productMapper.selectById(order.getProductId());
        if (product.getStock() < order.getQuantity()) return Result.fail(400, "库存不足");
        order.setPayStatus("PAID");
        order.setOrderStatus("PAID_PENDING_SHIP");
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);
        product.setStock(product.getStock() - order.getQuantity());
        product.setSaleCount((product.getSaleCount() == null ? 0 : product.getSaleCount()) + order.getQuantity());
        if (product.getStock() <= 0) product.setPublishStatus("SOLD_OUT");
        productMapper.updateById(product);
        return Result.success("支付成功", null);
    }

    @PostMapping("/orders/{id}/mock-ship")
    public Result<String> mockShip(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        Order order = orderMapper.selectById(id);
        if (order == null) return Result.fail(404, "订单不存在");
        if (!currentUserId().equals(order.getBuyerId()) && !currentUserId().equals(order.getSellerId())) {
            return Result.fail(403, "无权操作该订单");
        }
        if (!"PAID_PENDING_SHIP".equals(order.getOrderStatus())) {
            return Result.fail(400, "当前订单不可发货");
        }
        String company = body == null ? "平台模拟物流" : String.valueOf(body.getOrDefault("company", "平台模拟物流"));
        String trackingNo = body == null ? ("MOCK" + System.currentTimeMillis()) : String.valueOf(body.getOrDefault("trackingNo", "MOCK" + System.currentTimeMillis()));
        order.setOrderStatus("SHIPPED_PENDING_RECEIVE");
        order.setShipTime(LocalDateTime.now());
        order.setLogisticsCompany(company);
        order.setLogisticsNo(trackingNo);
        orderMapper.updateById(order);
        Integer logisticsCount = jdbcTemplate.queryForObject("select count(1) from logistics_info where order_id = ?", Integer.class, id);
        if (logisticsCount == null || logisticsCount == 0) {
            jdbcTemplate.update(
                    "insert into logistics_info(order_id, company, tracking_no, status, nodes_json, hazardous_notice, contact_name, contact_phone, last_updated_at) values(?,?,?,?,?,?,?,?,?)",
                    id, company, trackingNo, "已揽收", writeJson(Arrays.asList("已揽收")), "模拟物流信息", "平台调度中心", "400-800-1234", LocalDateTime.now()
            );
        } else {
            jdbcTemplate.update(
                    "update logistics_info set company=?, tracking_no=?, status=?, last_updated_at=? where order_id=?",
                    company, trackingNo, "已揽收", LocalDateTime.now(), id
            );
        }
        return Result.success("模拟发货成功", null);
    }
    @PostMapping("/orders/{id}/cancel")
    public Result<String> cancel(@PathVariable Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null || !currentUserId().equals(order.getBuyerId())) return Result.fail(404, "订单不存在");
        order.setOrderStatus("CANCELLED");
        order.setCancelTime(LocalDateTime.now());
        orderMapper.updateById(order);
        return Result.success("订单已取消", null);
    }

    @PostMapping("/orders/{id}/ship")
    public Result<String> ship(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        Order order = orderMapper.selectById(id);
        if (order == null || !currentUserId().equals(order.getSellerId())) return Result.fail(404, "订单不存在");
        order.setOrderStatus("SHIPPED_PENDING_RECEIVE");
        order.setShipTime(LocalDateTime.now());
        order.setLogisticsCompany(String.valueOf(body == null ? "平台物流" : body.getOrDefault("company", "平台物流")));
        order.setLogisticsNo(String.valueOf(body == null ? ("MOCK" + System.currentTimeMillis()) : body.getOrDefault("trackingNo", "MOCK" + System.currentTimeMillis())));
        orderMapper.updateById(order);
        jdbcTemplate.update("insert into logistics_info(order_id, company, tracking_no, status, nodes_json, hazardous_notice) values(?,?,?,?,?,?)",
                id, order.getLogisticsCompany(), order.getLogisticsNo(), "运输中", writeJson(Arrays.asList("已揽收", "运输中")), "模拟物流信息");
        return Result.success("已发货", null);
    }

    @PostMapping("/orders/{id}/confirm")
    public Result<String> confirmReceive(@PathVariable Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null || !currentUserId().equals(order.getBuyerId())) return Result.fail(404, "订单不存在");
        order.setOrderStatus("COMPLETED_PENDING_REVIEW");
        order.setReceiveTime(LocalDateTime.now());
        order.setCompleteTime(LocalDateTime.now());
        orderMapper.updateById(order);
        contractService.ensureContractForOrder(id);
        return Result.success("确认收货成功", null);
    }

    @DeleteMapping("/orders/{id}")
    public Result<String> deleteOrder(@PathVariable Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) return Result.fail(404, "订单不存在");
        if (currentUserId().equals(order.getBuyerId())) order.setBuyerDeleted(Boolean.TRUE);
        if (currentUserId().equals(order.getSellerId())) order.setSellerDeleted(Boolean.TRUE);
        orderMapper.updateById(order);
        return Result.success("订单已删除", null);
    }

    @PostMapping("/reviews")
    public Result<String> createReview(@RequestBody Map<String, Object> body) {
        Long orderId = Long.parseLong(String.valueOf(body.get("orderId")));
        Order order = orderMapper.selectById(orderId);
        if (order == null || !currentUserId().equals(order.getBuyerId())) return Result.fail(404, "订单不存在");
        Integer count = jdbcTemplate.queryForObject("select count(1) from product_review where order_id = ?", Integer.class, orderId);
        if (count != null && count > 0) return Result.fail(400, "该订单已评价");
        int score = Integer.parseInt(String.valueOf(body.getOrDefault("score", 5)));
        jdbcTemplate.update("insert into product_review(order_id, reviewer_id, seller_id, product_id, score, content, image_urls) values(?,?,?,?,?,?,?)",
                orderId, currentUserId(), order.getSellerId(), order.getProductId(), score, String.valueOf(body.getOrDefault("content", "")), writeJson(body.get("imageUrls")));
        order.setOrderStatus("COMPLETED");
        orderMapper.updateById(order);
        contractService.ensureContractForOrder(orderId);
        updateCredit(order.getSellerId(), score);
        return Result.success("评价成功", null);
    }

    @GetMapping("/reviews/product/{productId}")
    public Result<List<Map<String, Object>>> productReviews(@PathVariable Long productId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("select r.*, up.nickname as reviewerName, up.avatar as reviewerAvatar from product_review r left join user_profile up on r.reviewer_id = up.user_id where r.product_id = ? order by r.id desc", productId);
        for (Map<String, Object> row : rows) row.put("images", parseJsonArray(String.valueOf(row.get("image_urls"))));
        return Result.success(rows);
    }

    @GetMapping("/reviews/seller/{sellerId}/summary")
    public Result<Map<String, Object>> sellerReviewSummary(@PathVariable Long sellerId) {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        Integer total = jdbcTemplate.queryForObject("select count(1) from product_review where seller_id = ?", Integer.class, sellerId);
        Integer good = jdbcTemplate.queryForObject("select count(1) from product_review where seller_id = ? and score >= 4", Integer.class, sellerId);
        data.put("total", total == null ? 0 : total);
        data.put("goodRate", total == null || total == 0 ? 100 : Math.round((good * 100.0) / total));
        data.put("latest", jdbcTemplate.queryForList("select * from product_review where seller_id = ? order by id desc limit 5", sellerId));
        return Result.success(data);
    }

    @PostMapping("/reviews/{id}/reply")
    public Result<String> replyReview(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        jdbcTemplate.update("update product_review set reply_content = ? where id = ? and seller_id = ?", String.valueOf(body.getOrDefault("replyContent", "")), id, currentUserId());
        return Result.success("回复成功", null);
    }

    @PostMapping("/messages/send")
    public Result<String> sendMessage(@RequestBody Map<String, Object> body) {
        Long productId = Long.parseLong(String.valueOf(body.get("productId")));
        Long receiverId = Long.parseLong(String.valueOf(body.get("receiverId")));
        String sessionId = sessionId(currentUserId(), receiverId, productId);
        jdbcTemplate.update("insert into trade_message(session_id, product_id, sender_id, receiver_id, message_type, content, read_flag) values(?,?,?,?,?,?,0)",
                sessionId, productId, currentUserId(), receiverId, String.valueOf(body.getOrDefault("messageType", "TEXT")), String.valueOf(body.getOrDefault("content", "")));
        return Result.success("发送成功", null);
    }

    @GetMapping("/messages/sessions")
    public Result<List<Map<String, Object>>> sessions() {
        return Result.success(jdbcTemplate.queryForList("select session_id as sessionId, product_id as productId, max(id) as latestId, sum(case when receiver_id = ? and read_flag = 0 then 1 else 0 end) as unreadCount, max(content) as latestContent from trade_message where sender_id = ? or receiver_id = ? group by session_id, product_id order by latestId desc", currentUserId(), currentUserId(), currentUserId()));
    }

    @GetMapping("/messages/history")
    public Result<List<Map<String, Object>>> history(@RequestParam Long productId, @RequestParam Long otherUserId) {
        String sessionId = sessionId(currentUserId(), otherUserId, productId);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("select * from trade_message where session_id = ? order by id asc", sessionId);
        jdbcTemplate.update("update trade_message set read_flag = 1 where session_id = ? and receiver_id = ?", sessionId, currentUserId());
        return Result.success(rows);
    }

    @GetMapping("/sellers/{sellerId}")
    public Result<Map<String, Object>> sellerHome(@PathVariable Long sellerId) {
        Map<String, Object> data = singleMap("select u.id, u.username, u.real_name as realName, u.created_at as registerTime, up.nickname, up.avatar, up.bio, ifnull(cs.score,100) as creditScore from user u left join user_profile up on u.id = up.user_id left join credit_score cs on u.id = cs.user_id where u.id = ?", sellerId);
        if (data == null) return Result.fail(404, "卖家不存在");
        Integer totalSales = jdbcTemplate.queryForObject("select ifnull(sum(quantity), 0) from `order` where seller_id = ? and order_status in ('COMPLETED','COMPLETED_PENDING_REVIEW')", Integer.class, sellerId);
        data.put("totalSales", totalSales == null ? 0 : totalSales);
        data.put("products", jdbcTemplate.queryForList("select id, title, price, cover_image as coverImage, health_level as healthLevel, shipping_from as shippingFrom from product where seller_id = ? and publish_status = 'ON_SHELF' and deleted_flag = 0 order by id desc", sellerId));
        data.put("reviewSummary", sellerReviewSummary(sellerId).getData());
        return Result.success(data);
    }
    @GetMapping({"/profile", "/profile/{userId}"})
    public Result<Map<String, Object>> profile(@PathVariable(required = false) Long userId) {
        Long targetId = userId == null ? currentUserId() : userId;
        ensureUserProfile(targetId);
        Map<String, Object> data = singleMap("select u.id, u.username, u.real_name as realName, up.nickname, up.avatar, up.bio, up.city, ifnull(cs.score,100) as creditScore from user u left join user_profile up on u.id = up.user_id left join credit_score cs on u.id = cs.user_id where u.id = ?", targetId);
        data.put("favorites", jdbcTemplate.queryForObject("select count(1) from favorite_product where user_id = ?", Integer.class, targetId));
        data.put("cartCount", jdbcTemplate.queryForObject("select count(1) from cart_item where user_id = ?", Integer.class, targetId));
        data.put("products", jdbcTemplate.queryForList("select id, title, price, publish_status as publishStatus, cover_image as coverImage from product where seller_id = ? and deleted_flag = 0 order by id desc", targetId));
        data.put("receivedReviews", jdbcTemplate.queryForList("select * from product_review where seller_id = ? order by id desc limit 10", targetId));
        return Result.success(data);
    }

    @PutMapping("/profile")
    public Result<String> updateProfile(@RequestBody Map<String, Object> body) {
        ensureUserProfile(currentUserId());
        jdbcTemplate.update("update user_profile set nickname = ?, avatar = ?, bio = ?, city = ? where user_id = ?",
                body.get("nickname"), body.get("avatar"), body.get("bio"), body.get("city"), currentUserId());
        return Result.success("资料已更新", null);
    }

    private Long currentUserId() {
        Long userId = AuthUserContext.getCurrentUserId();
        return userId == null ? 1L : userId;
    }

    private String resolveProductSort(String sortBy) {
        if ("priceAsc".equalsIgnoreCase(sortBy)) return " p.price asc ";
        if ("priceDesc".equalsIgnoreCase(sortBy)) return " p.price desc ";
        if ("credit".equalsIgnoreCase(sortBy)) return " creditScore desc, p.id desc ";
        if ("latest".equalsIgnoreCase(sortBy)) return " p.id desc ";
        return " p.favorite_count desc, p.id desc ";
    }

    private Product toProduct(Map<String, Object> body) {
        Product product = new Product();
        if (body.get("batteryId") != null) product.setBatteryId(Long.parseLong(String.valueOf(body.get("batteryId"))));
        product.setTitle(String.valueOf(body.getOrDefault("title", "二手电池商品")));
        product.setDescription(String.valueOf(body.getOrDefault("description", "")));
        if (body.get("price") != null) product.setPrice(new BigDecimal(String.valueOf(body.get("price"))));
        if (body.get("originalPrice") != null) product.setOriginalPrice(new BigDecimal(String.valueOf(body.get("originalPrice"))));
        if (body.get("stock") != null) product.setStock(Integer.parseInt(String.valueOf(body.get("stock"))));
        product.setCoverImage(String.valueOf(body.getOrDefault("coverImage", "https://dummyimage.com/600x420/e8eef8/4a6480&text=EV+Battery")));
        product.setImageUrls(writeJson(body.get("imageUrls") == null ? Arrays.asList(product.getCoverImage()) : body.get("imageUrls")));
        product.setShippingFrom(String.valueOf(body.getOrDefault("shippingFrom", "上海")));
        product.setShippingType(String.valueOf(body.getOrDefault("shippingType", "物流")));
        product.setBatteryType(String.valueOf(body.getOrDefault("batteryType", "磷酸铁锂")));
        product.setPublishStatus(body.get("publishStatus") == null ? null : String.valueOf(body.get("publishStatus")));
        if (body.get("isFreeShipping") != null) product.setIsFreeShipping(Boolean.parseBoolean(String.valueOf(body.get("isFreeShipping"))));
        return product;
    }

    private void fillBatteryDerivedFields(Product product) {
        HealthAssessment assessment = healthAssessmentMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<HealthAssessment>().eq(HealthAssessment::getBatteryId, product.getBatteryId()).orderByDesc(HealthAssessment::getId).last("limit 1"));
        if (assessment != null) product.setHealthLevel(assessment.getHealthLevel());
        if (!StringUtils.hasText(product.getTitle())) product.setTitle("优选退役电池-" + (product.getHealthLevel() == null ? "待评估" : product.getHealthLevel()));
    }

    private Map<String, Object> latestAssessment(Long batteryId) {
        return singleMap("select health_score as healthScore, health_level as healthLevel, suggested_scene as suggestedScene, llm_summary as llmSummary from health_assessment where battery_id = ? order by id desc limit 1", batteryId);
    }

    private Map<String, Object> productDetailMap(Long id) {
        return singleMap("select * from product where id = ?", id);
    }

    private Map<String, Object> orderDetailMap(Long id) {
        Map<String, Object> row = singleMap("select * from `order` where id = ?", id);
        if (row == null) return null;
        row.put("addressSnapshot", parseJsonObject(String.valueOf(row.get("address_snapshot"))));
        row.put("productSnapshot", parseJsonObject(String.valueOf(row.get("product_snapshot"))));
        row.put("review", singleMap("select score, content, created_at as createdAt from product_review where order_id = ?", id));
        row.put("canPay", "PENDING_PAYMENT".equals(String.valueOf(row.get("order_status"))));
        row.put("canShip", "PAID_PENDING_SHIP".equals(String.valueOf(row.get("order_status"))));
        row.put("canConfirm", "SHIPPED_PENDING_RECEIVE".equals(String.valueOf(row.get("order_status"))));
        return row;
    }

    private Map<String, Object> defaultAddress() {
        return singleMap("select * from user_address where user_id = ? order by is_default desc, id desc limit 1", currentUserId());
    }

    private boolean isFavorite(Long userId, Long productId) {
        Integer count = jdbcTemplate.queryForObject("select count(1) from favorite_product where user_id = ? and product_id = ?", Integer.class, userId, productId);
        return count != null && count > 0;
    }

    private void ensureUserProfile(Long userId) {
        Integer count = jdbcTemplate.queryForObject("select count(1) from user_profile where user_id = ?", Integer.class, userId);
        if (count == null || count == 0) {
            Map<String, Object> user = singleMap("select username, real_name from user where id = ?", userId);
            jdbcTemplate.update("insert into user_profile(user_id, nickname, avatar, bio, city) values(?,?,?,?,?)", userId,
                    user == null ? ("用户" + userId) : String.valueOf(user.get("username")),
                    "https://dummyimage.com/120x120/e8eef8/4a6480&text=U",
                    "这个人很低调，什么也没有留下。",
                    "上海");
        }
    }

    private void updateCredit(Long userId, int score) {
        Integer count = jdbcTemplate.queryForObject("select count(1) from credit_score where user_id = ?", Integer.class, userId);
        if (count == null || count == 0) jdbcTemplate.update("insert into credit_score(user_id, score, level, updated_reason) values(?,?,?,?)", userId, 100, "A", "init");
        int delta = score >= 4 ? 2 : (score == 3 ? 0 : -4);
        jdbcTemplate.update("update credit_score set score = greatest(0, score + ?), updated_reason = ? where user_id = ?", delta, "评价更新", userId);
    }

    private String sessionId(Long a, Long b, Long productId) {
        long left = Math.min(a, b);
        long right = Math.max(a, b);
        return left + "_" + right + "_" + productId;
    }

    private Map<String, Object> singleMap(String sql, Object... args) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, args);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? new LinkedHashMap<String, Object>() : value);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private List<Object> parseJsonArray(String json) {
        try {
            if (!StringUtils.hasText(json) || "null".equalsIgnoreCase(json)) return new ArrayList<Object>();
            return objectMapper.readValue(json, new TypeReference<List<Object>>() {});
        } catch (Exception ex) {
            return new ArrayList<Object>();
        }
    }

    private Map<String, Object> parseJsonObject(String json) {
        try {
            if (!StringUtils.hasText(json) || "null".equalsIgnoreCase(json)) return new LinkedHashMap<String, Object>();
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            return new LinkedHashMap<String, Object>();
        }
    }
}
