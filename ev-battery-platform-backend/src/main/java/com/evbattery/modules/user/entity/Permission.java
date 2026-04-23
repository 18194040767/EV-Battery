package com.evbattery.modules.user.entity;
        import com.baomidou.mybatisplus.annotation.TableName;
        import com.evbattery.common.BaseEntity;
        import lombok.Data;
        import lombok.EqualsAndHashCode;
        @Data
        @EqualsAndHashCode(callSuper = true)
        @TableName("permission")
        public class Permission extends BaseEntity {
            private Long id;
private String permCode;
private String permName;
private String permType;
private String path;
        }
