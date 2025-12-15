package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 *
 * </p>
 *
 * @author lzx
 * @since 2024-04-27
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "ReportCarIn对象", description = "")
public class ReportCarIn implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "最终车牌号")
    private String carLicenseNumber;

    @ApiModelProperty(value = "进场通道名称")
    private String enterChannelName;

    @ApiModelProperty(value = "入场时间")
    private String enterTime;

    @ApiModelProperty(value = "进场类型（正常进出/免费进出/异常进出）")
    private String enterType;

    @ApiModelProperty(value = "进场Vip类型（月卡/临时卡/免费卡/年卡/季卡/日卡/贵宾卡等）")
    private String enterVipType;

    @ApiModelProperty(value = "入场车牌颜色（黑色/白色/红色/蓝色/黄色/绿色/其他）")
    private String enterCarLicenseColor;

    @ApiModelProperty(value = "车辆类型（小型车/大型车/摩托车/电动车/货车/客车/特种车辆等）")
    private String enterCarType;

    @ApiModelProperty(value = "进场VIP名称")
    private String enterCustomVipName;

    @ApiModelProperty(value = "进场车辆全图")
    private String enterCarFullPicture;

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