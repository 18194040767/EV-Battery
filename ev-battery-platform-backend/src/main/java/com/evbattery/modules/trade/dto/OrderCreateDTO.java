package com.evbattery.modules.trade.dto;
import lombok.Data;
@Data
public class OrderCreateDTO {
    private Long id;
    private String keyword;
    private String remark;
}
