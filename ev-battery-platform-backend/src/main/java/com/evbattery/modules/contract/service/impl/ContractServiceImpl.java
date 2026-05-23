package com.evbattery.modules.contract.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.evbattery.common.util.HashUtil;
import com.evbattery.common.util.PdfGenerator;
import com.evbattery.modules.contract.entity.Contract;
import com.evbattery.modules.contract.mapper.ContractMapper;
import com.evbattery.modules.contract.service.ContractService;
import com.evbattery.modules.trade.entity.Order;
import com.evbattery.modules.trade.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ContractServiceImpl implements ContractService {

    @Resource
    private ContractMapper contractMapper;

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Value("${app.storage.root:./storage}")
    private String storageRoot;

    @Override
    public Map<String, Object> ensureContractForOrder(Long orderId) {
        Contract existing = contractMapper.selectOne(new LambdaQueryWrapper<Contract>().eq(Contract::getOrderId, orderId).last("limit 1"));
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (existing != null && StringUtils.hasText(existing.getPdfPath()) && Files.exists(Paths.get(existing.getPdfPath()))) {
            return toDetail(existing);
        }
        Map<String, Object> joined = loadOrderJoined(orderId);
        if (joined == null) {
            throw new IllegalArgumentException("订单数据不完整");
        }
        String contractNo = "CT" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + orderId;
        String contentPayload = String.valueOf(order.getOrderNo()) + "|" + order.getBuyerId() + "|" + order.getSellerId() + "|" + order.getAmount() + "|" + contractNo + "|" + LocalDateTime.now();
        String contentHash = HashUtil.sha256(contentPayload);

        List<String> lines = new ArrayList<String>();
        lines.add("合同编号：" + contractNo);
        lines.add("生成时间：" + LocalDateTime.now());
        lines.add("买方：" + value(joined.get("buyerName")) + "（用户ID：" + order.getBuyerId() + "，电话：" + maskPhone(joined.get("buyerPhone")) + "）");
        lines.add("卖方：" + value(joined.get("sellerName")) + "（用户ID：" + order.getSellerId() + "，电话：" + maskPhone(joined.get("sellerPhone")) + "）");
        lines.add("订单编号：" + value(order.getOrderNo()));
        lines.add("商品名称：" + value(joined.get("productTitle")));
        lines.add("电池档案编号：" + value(joined.get("batteryCode")));
        lines.add("健康评分：" + value(joined.get("healthScore")));
        lines.add("交易数量：" + value(order.getQuantity()));
        lines.add("商品单价：" + value(order.getUnitPrice()) + " 元");
        lines.add("交易总额：" + value(order.getAmount()) + " 元");
        lines.add("支付时间：" + value(order.getPayTime()));
        lines.add("完成时间：" + value(order.getCompleteTime()));
        lines.add("履约约定：买卖双方确认该订单对应电池商品已完成平台交易流程，商品信息、价格、数量与订单记录一致。");
        lines.add("存证说明：本合同 PDF 生成后计算 SHA-256 摘要并保存，可在合同查验页上传文件核验是否被篡改。");
        lines.add("平台声明：平台提供交易撮合、合同生成和存证校验服务，实际商品交付、质量承诺与售后责任以订单及双方约定为准。");

        byte[] pdfBytes = PdfGenerator.generateSimpleDocument("动力电池电子交易合同", lines);
        String pdfHash = HashUtil.sha256(pdfBytes);
        File pdfFile = Paths.get(storageRoot, "contracts", contractNo + ".pdf").toFile();
        PdfGenerator.writeToFile(pdfFile, pdfBytes);

        Contract contract = existing == null ? new Contract() : existing;
        contract.setOrderId(orderId);
        contract.setContractNo(contractNo);
        contract.setPdfPath(pdfFile.getAbsolutePath());
        contract.setContentHash(contentHash);
        contract.setPdfHash(pdfHash);
        contract.setHashDigest(pdfHash);
        contract.setVerifyCount(contract.getVerifyCount() == null ? 0 : contract.getVerifyCount());
        contract.setNotarizationTxId("SHA256-" + pdfHash.substring(0, 16));
        if (contract.getId() == null) {
            contractMapper.insert(contract);
        } else {
            contractMapper.updateById(contract);
        }
        if (contract.getId() == null) {
            contract = contractMapper.selectOne(new LambdaQueryWrapper<Contract>().eq(Contract::getOrderId, orderId).last("limit 1"));
        }
        return toDetail(contract);
    }

    @Override
    public Map<String, Object> listContracts(Long currentUserId, boolean admin, Integer page, Integer size) {
        ensureEligibleContracts(currentUserId, admin);
        int currentPage = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : size;
        List<Object> args = new ArrayList<Object>();
        StringBuilder where = new StringBuilder(" where 1=1 ");
        if (!admin) {
            where.append(" and (o.buyer_id = ? or o.seller_id = ?) ");
            args.add(currentUserId);
            args.add(currentUserId);
        }
        Integer total = jdbcTemplate.queryForObject(
                "select count(1) from contract c left join `order` o on c.order_id = o.id" + where,
                Integer.class,
                args.toArray()
        );
        List<Object> queryArgs = new ArrayList<Object>(args);
        queryArgs.add((currentPage - 1) * pageSize);
        queryArgs.add(pageSize);
        List<Map<String, Object>> records = jdbcTemplate.queryForList(
                "select c.id, c.contract_no as contractNo, c.order_id as orderId, c.hash_digest as hashDigest, c.content_hash as contentHash, c.pdf_hash as pdfHash, " +
                        "c.verify_count as verifyCount, c.created_at as createdAt, o.order_no as orderNo, p.title as productTitle, buyer.username as buyerName, seller.username as sellerName " +
                        "from contract c " +
                        "left join `order` o on c.order_id = o.id " +
                        "left join product p on o.product_id = p.id " +
                        "left join `user` buyer on o.buyer_id = buyer.id " +
                        "left join `user` seller on o.seller_id = seller.id " +
                        where + " order by c.id desc limit ?, ?",
                queryArgs.toArray()
        );
        for (Map<String, Object> record : records) {
            record.put("downloadUrl", "/api/contract/" + record.get("id") + "/download");
        }
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("records", records);
        data.put("total", total == null ? 0 : total);
        return data;
    }

    @Override
    public Map<String, Object> verifyContract(String contractNo, byte[] uploadBytes) {
        Contract contract = contractMapper.selectOne(new LambdaQueryWrapper<Contract>().eq(Contract::getContractNo, contractNo).last("limit 1"));
        if (contract == null) {
            throw new IllegalArgumentException("合同不存在");
        }
        String currentHash = HashUtil.sha256(uploadBytes == null || uploadBytes.length == 0 ? readFile(contract.getPdfPath()) : uploadBytes);
        boolean valid = currentHash.equalsIgnoreCase(contract.getPdfHash());
        contract.setVerifyCount((contract.getVerifyCount() == null ? 0 : contract.getVerifyCount()) + 1);
        contractMapper.updateById(contract);
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("contractNo", contract.getContractNo());
        data.put("valid", valid);
        data.put("storedHash", contract.getPdfHash());
        data.put("currentHash", currentHash);
        data.put("message", valid ? "合同哈希一致，文件未被篡改" : "合同哈希不一致，请核查文件来源");
        return data;
    }

    @Override
    public Map<String, Object> verifyContractById(Long contractId) {
        Contract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw new IllegalArgumentException("合同不存在");
        }
        return verifyContract(contract.getContractNo(), null);
    }

    @Override
    public byte[] loadContractPdf(Long contractId, Long currentUserId, boolean admin) {
        Contract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw new IllegalArgumentException("合同不存在");
        }
        Order order = orderMapper.selectById(contract.getOrderId());
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (!admin && !currentUserId.equals(order.getBuyerId()) && !currentUserId.equals(order.getSellerId())) {
            throw new IllegalArgumentException("无权访问该合同");
        }
        ensureContractForOrder(contract.getOrderId());
        contract = contractMapper.selectById(contractId);
        return readFile(contract.getPdfPath());
    }

    private Map<String, Object> loadOrderJoined(Long orderId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select p.title as productTitle, b.battery_code as batteryCode, ha.health_score as healthScore, " +
                        "buyer.username as buyerName, buyer.phone as buyerPhone, seller.username as sellerName, seller.phone as sellerPhone " +
                        "from `order` o " +
                        "left join product p on o.product_id = p.id " +
                        "left join battery_record b on p.battery_id = b.id " +
                        "left join health_assessment ha on ha.battery_id = b.id " +
                        "left join `user` buyer on o.buyer_id = buyer.id " +
                        "left join `user` seller on o.seller_id = seller.id " +
                        "where o.id = ? order by ha.id desc limit 1",
                orderId
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    private Map<String, Object> toDetail(Contract contract) {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("id", contract.getId());
        data.put("contractNo", contract.getContractNo());
        data.put("orderId", contract.getOrderId());
        data.put("hashDigest", contract.getHashDigest());
        data.put("contentHash", contract.getContentHash());
        data.put("pdfHash", contract.getPdfHash());
        data.put("verifyCount", contract.getVerifyCount() == null ? 0 : contract.getVerifyCount());
        data.put("downloadUrl", "/api/contract/" + contract.getId() + "/download");
        Map<String, Object> rows = loadContractListRow(contract.getId());
        if (rows != null) {
            data.putAll(rows);
        }
        return data;
    }

    private Map<String, Object> loadContractListRow(Long contractId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select c.id, c.contract_no as contractNo, c.order_id as orderId, c.hash_digest as hashDigest, c.content_hash as contentHash, c.pdf_hash as pdfHash, " +
                        "c.verify_count as verifyCount, c.created_at as createdAt, o.order_no as orderNo, p.title as productTitle, buyer.username as buyerName, seller.username as sellerName " +
                        "from contract c " +
                        "left join `order` o on c.order_id = o.id " +
                        "left join product p on o.product_id = p.id " +
                        "left join `user` buyer on o.buyer_id = buyer.id " +
                        "left join `user` seller on o.seller_id = seller.id " +
                        "where c.id = ? limit 1",
                contractId
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    private void ensureEligibleContracts(Long currentUserId, boolean admin) {
        List<Map<String, Object>> rows;
        if (admin) {
            rows = jdbcTemplate.queryForList("select id from `order` where order_status in ('COMPLETED_PENDING_REVIEW','COMPLETED') order by id desc limit 24");
        } else {
            rows = jdbcTemplate.queryForList(
                    "select id from `order` where (buyer_id = ? or seller_id = ?) and order_status in ('COMPLETED_PENDING_REVIEW','COMPLETED') order by id desc limit 24",
                    currentUserId, currentUserId
            );
        }
        for (Map<String, Object> row : rows) {
            try {
                ensureContractForOrder(Long.parseLong(String.valueOf(row.get("id"))));
            } catch (Exception ignored) {
            }
        }
    }

    private String maskPhone(Object phone) {
        String raw = phone == null ? "" : String.valueOf(phone);
        if (raw.length() < 7) {
            return raw.isEmpty() ? "-" : raw;
        }
        return raw.substring(0, 3) + "****" + raw.substring(raw.length() - 4);
    }

    private String value(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private byte[] readFile(String path) {
        try {
            return Files.readAllBytes(Paths.get(path));
        } catch (Exception ex) {
            throw new IllegalStateException("读取合同文件失败", ex);
        }
    }
}
