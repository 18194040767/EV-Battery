package com.evbattery.modules.trade.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.evbattery.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("product")
public class Product extends BaseEntity {
    private Long id;
    private Long sellerId;
    private Long batteryId;
    private String title;
    private String description;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stock;
    private Integer status;
    private String coverImage;
    private String imageUrls;
    private String shippingFrom;
    private String shippingType;
    private String batteryType;
    private String healthLevel;
    private Boolean isFreeShipping;
    private Integer saleCount;
    private Integer viewCount;
    private Integer favoriteCount;
    private String publishStatus;
    private String auditStatus;
    private Boolean draftFlag;
    private Boolean deletedFlag;
}

