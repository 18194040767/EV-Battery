package com.evbattery.modules.trade.entity;
        import com.baomidou.mybatisplus.annotation.TableName;
        import com.evbattery.common.BaseEntity;
        import lombok.Data;
        import lombok.EqualsAndHashCode;
        @Data
        @EqualsAndHashCode(callSuper = true)
        @TableName("purchase_demand")
        public class PurchaseDemand extends BaseEntity {
            private Long id;
private Long buyerId;
private String title;
private String requirement;
private java.math.BigDecimal budgetMin;
private java.math.BigDecimal budgetMax;
private Integer status;
        }
