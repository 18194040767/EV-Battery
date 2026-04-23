package com.evbattery.modules.user.entity;
        import com.baomidou.mybatisplus.annotation.TableName;
        import com.evbattery.common.BaseEntity;
        import lombok.Data;
        import lombok.EqualsAndHashCode;
        @Data
        @EqualsAndHashCode(callSuper = true)
        @TableName("role")
        public class Role extends BaseEntity {
            private Long id;
private String roleCode;
private String roleName;
private String description;
        }
