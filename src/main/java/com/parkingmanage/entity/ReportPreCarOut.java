package com.parkingmanage.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 预出场报告实体类
 * </p>
 *
 * @author System
 * @since 2025-06-25
 */
@Data
@ApiModel(value="ReportPreCarOut对象", description="预出场报告")
public class ReportPreCarOut implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "离场时间")
    private String leaveTime;

    @ApiModelProperty(value = "离场通道代码")
    private String leaveChannelCode;

    @ApiModelProperty(value = "离场车牌类型")
    private Integer leaveCarLicenseType;

    @ApiModelProperty(value = "应收金额")
    private Double amountReceivable;

    @ApiModelProperty(value = "离场收费组代码")
    private String leaveChargeGroupCode;

//    @ApiModelProperty(value = "离场图片数组")
//    private List<LeaveImageArray> leaveImageArray;

    @ApiModelProperty(value = "扩展信息")
    private Map<String, Object> extendInfo;

    @ApiModelProperty(value = "停车场代码")
    private String parkingCode;

    @ApiModelProperty(value = "离场车辆类型")
    private Integer leaveCarType;

    @ApiModelProperty(value = "离场通道ID")
    private Integer leaveChannelId;

    @ApiModelProperty(value = "总金额")
    private Double totalAmount;

    @ApiModelProperty(value = "离场车牌颜色（0未定义，1蓝色，2黄色，3白色，4黑色，5绿色）")
    private Integer leaveCarLicenseColor;

    @ApiModelProperty(value = "命令")
    private String cmd;

    @ApiModelProperty(value = "离场车牌号")
    private String leaveCarLicenseNumber;

    @ApiModelProperty(value = "锁定key")
    private String lockKey;

    @ApiModelProperty(value = "停车场编码")
    private String parkCode;

    @ApiModelProperty(value = "离场通道自定义代码")
    private String leaveChannelCustomCode;

    @ApiModelProperty(value = "进场时间")
    private String enterTime;
}