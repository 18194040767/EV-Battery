package com.evbattery.modules.battery.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.evbattery.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tag")
public class Tag extends BaseEntity {
    private Long id;
    private String name;
    private String color;
}
