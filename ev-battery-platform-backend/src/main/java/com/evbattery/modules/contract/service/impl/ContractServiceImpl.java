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
        if (existing != null && StringUtils.hasText(existing.getPdfPath()) && new File(existing.getPdfPath()).exists()) {
            return toDetail(existing);
        }
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        Map<String, Object> joined = loadOrderJoined(orderId);
        if (joined == null) {
            throw new IllegalArgumentException("订单数据不完整");
        }
        String contractNo = "CT" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + orderId;
        String contentPayload = String.valueOf(order.getOrderNo()) + "|" + order.getBuyerId() + "|" + order.getSellerId() + "|" + order.getAmount() + "|" + contractNo + "|" + LocalDateTime.now();
        String contentHash = HashUtil.sha256(contentPayload);

        List<String> lines = new ArrayList<String>();
        lines.add("Contract No: " + contractNo);
        lines.add("Generated At: " + LocalDateTime.now());
        lines.add("Buyer: " + value(joined.get("buyerName")) + " / ID " + order.getBuyerId() + " / Phone " + maskPhone(joined.get("buyerPhone")));
        lines.add("Seller: " + value(joined.get("sellerName")) + " / ID " + order.getSellerId() + " / Phone " + maskPhone(joined.get("sellerPhone")));
        lines.add("Battery Code: " + value(joined.get("batteryCode")));
        lines.add("Health Score: " + value(joined.get("healthScore")));
        lines.add("Product: " + value(joined.get("productTitle")));
        lines.add("Order No: " + value(order.getOrderNo()));
        lines.add("Quantity: " + value(order.getQuantity()));
        lines.add("Unit Price: " + value(order.getUnitPrice()));
        lines.add("Total Amount: " + value(order.getAmount()));
        lines.add("Pay Time: " + value(order.getPayTime()));
        lines.add("Complete Time: " + value(order.getCompleteTime()));
        lines.add("Disclaimer: The platform provides transaction matching and notarization services only.");
        lines.add("Integrity Rule: SHA-256 digest is used to verify that the contract remains unchanged.");

        byte[] pdfBytes = PdfGenerator.generateSimpleDocument("EV Battery Electronic Contract", lines);
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
        return data;
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
