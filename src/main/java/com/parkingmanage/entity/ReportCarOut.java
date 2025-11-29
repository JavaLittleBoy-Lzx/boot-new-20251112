package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * 
 * </p>
 *
 * @author lzx
 * @since 2024-04-27
 */
@Data
@Slf4j
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="ReportCarOut对象", description="")
public class ReportCarOut implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "最终车牌号")
    private String carLicenseNumber;

    @ApiModelProperty(value = "进场通道名称")
    private String enterChannelName;

    @ApiModelProperty(value = "出场通道名，路边停车场没有")
    private String leaveChannelName;

    @ApiModelProperty(value = "入场时间")
    private String enterTime;

    @ApiModelProperty(value = "离场时间")
    private String leaveTime;

    @ApiModelProperty(value = "进场类型（正常进出/免费进出/异常进出）")
    private String enterType;

    @ApiModelProperty(value = "进场Vip类型（月卡/临时卡/免费卡等）")
    private String enterVipType;

    @ApiModelProperty(value = "出场Vip类型（月卡/临时卡/免费卡等）")
    private String leaveVipType;

    @ApiModelProperty(value = "离场VIP名称")
    private String leaveCustomVipName;

    @ApiModelProperty(value = "应收金额")
    private String amountReceivable;

    @ApiModelProperty(value = "离场车辆全图")
    private String leaveCarFullPicture;

    @ApiModelProperty(value = "进场车辆全图")
    private String enterCarFullPicture;

    @ApiModelProperty(value = "离场类型（正常进出/免费进出/异常进出）")
    private String leaveType;

    @ApiModelProperty(value = "入场车牌颜色（黑色/白色/红色/蓝色/黄色/绿色/其他）")
    private String enterCarLicenseColor;

    @ApiModelProperty(value = "离场车牌颜色（黑色/白色/红色/蓝色/黄色/绿色/其他）")
    private String leaveCarLicenseColor;

    @ApiModelProperty(value = "入场车辆类型（小型车/大型车/摩托车）")
    private String enterCarType;

    @ApiModelProperty(value = "离场车辆类型（小型车/大型车/摩托车）")
    private String leaveCarType;

    @ApiModelProperty(value = "记录类型（正常记录/异常记录）")
    private String recordType;

    @ApiModelProperty(value = "停车时长（秒）")
    private String stoppingTime;


    @ApiModelProperty(value = "备注")
    private String remark;

    @TableLogic(value="0",delval="1")
    @ApiModelProperty(value = "逻辑删除标识：0：未删除，1：已删除")
    private Integer deleted;

    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @ApiModelProperty(value = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date updateTime;
}
