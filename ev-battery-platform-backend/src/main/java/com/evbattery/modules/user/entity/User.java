package com.evbattery.modules.user.entity;
        import com.baomidou.mybatisplus.annotation.TableName;
        import com.evbattery.common.BaseEntity;
        import lombok.Data;
        import lombok.EqualsAndHashCode;
        @Data
        @EqualsAndHashCode(callSuper = true)
        @TableName("user")
        public class User extends BaseEntity {
            private Long id;
private String username;
private String password;
private String realName;
private String phone;
private String email;
private Integer status;
        }
