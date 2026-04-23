package com.evbattery.modules.logistics.entity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.evbattery.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("logistics_info")
public class LogisticsInfo extends BaseEntity {
    private Long id;
    private Long orderId;
    private String company;
    private String trackingNo;
    private String status;
    private String nodesJson;
    private String hazardousNotice;
    private String noticePdfPath;
    private String contactName;
    private String contactPhone;
    @TableField("last_updated_at")
    private LocalDateTime lastUpdatedAt;
}
