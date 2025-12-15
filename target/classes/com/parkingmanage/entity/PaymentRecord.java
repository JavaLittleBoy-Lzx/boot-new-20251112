package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * 缴费记录实体类
 * </p>
 *
 * @author system
 * @since 2025-01-27
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "PaymentRecord对象", description = "缴费记录")
public class PaymentRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "支付方式")
    private String paymentMode;

    @ApiModelProperty(value = "支付方式备注")
    private String paymentModeRemark;

    @ApiModelProperty(value = "支付来源")
    private String payOrigin;

    @ApiModelProperty(value = "支付来源备注")
    private String payOriginRemark;

    @ApiModelProperty(value = "支付时间")
    private String payTime;

    @ApiModelProperty(value = "支付状态")
    private String payStatus;

    @ApiModelProperty(value = "实际应收金额")
    private String actualReceivable;

    @ApiModelProperty(value = "应收金额")
    private String amountReceivable;

    @ApiModelProperty(value = "车牌号码")
    private String carPlateNumber;

    @ApiModelProperty(value = "停车时长（格式：xx小时xx分钟xx秒，若小时为0则不显示，分钟为0但小时不为0时显示，秒同理）")
    private String parkingDuration;

    @TableLogic(value = "0", delval = "1")
    @ApiModelProperty(value = "逻辑删除标识：0：未删除，1：已删除")
    private Integer deleted;

    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @ApiModelProperty(value = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
}
