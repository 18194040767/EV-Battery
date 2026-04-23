package com.evbattery.modules.trade.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.evbattery.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("`order`")
public class Order extends BaseEntity {
    private Long id;
    private String orderNo;
    private Long productId;
    private Long buyerId;
    private Long sellerId;
    private Integer quantity;
    private BigDecimal amount;
    private BigDecimal unitPrice;
    private String addressSnapshot;
    private String productSnapshot;
    private String orderStatus;
    private String payStatus;
    private String paymentMethod;
    private Boolean buyerDeleted;
    private Boolean sellerDeleted;
    private LocalDateTime payTime;
    private LocalDateTime shipTime;
    private LocalDateTime receiveTime;
    private LocalDateTime completeTime;
    private LocalDateTime cancelTime;
    private LocalDateTime refundTime;
    private String logisticsCompany;
    private String logisticsNo;
    private String remark;
}

