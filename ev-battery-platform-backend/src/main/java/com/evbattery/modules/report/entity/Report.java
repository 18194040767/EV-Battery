package com.evbattery.modules.report.entity;
        import com.baomidou.mybatisplus.annotation.TableName;
        import com.evbattery.common.BaseEntity;
        import lombok.Data;
        import lombok.EqualsAndHashCode;
        @Data
        @EqualsAndHashCode(callSuper = true)
        @TableName("report")
        public class Report extends BaseEntity {
            private Long id;
private String relatedType;
private Long relatedId;
private String versionNo;
private String content;
private String summary;
private Long createdBy;
        }
