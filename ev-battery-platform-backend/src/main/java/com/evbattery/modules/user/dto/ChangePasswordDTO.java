package com.evbattery.modules.user.dto;
import lombok.Data;
@Data
public class ChangePasswordDTO {
    private Long id;
    private String keyword;
    private String remark;
}
