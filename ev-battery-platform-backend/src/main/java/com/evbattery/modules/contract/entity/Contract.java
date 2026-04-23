package com.evbattery.modules.contract.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.evbattery.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("contract")
public class Contract extends BaseEntity {
    private Long id;
    private String contractNo;
    private Long orderId;
    private String pdfPath;
    private String hashDigest;
    private String contentHash;
    private String pdfHash;
    private Integer verifyCount;
    private String notarizationTxId;
}
