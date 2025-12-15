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
 * ACMS数据推送事件记录实体
 * 用于存储ACMS系统推送的进出事件数据
 * 
 * @author System
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("acms_event_record")
@ApiModel(value = "AcmsEventRecord对象", description = "ACMS数据推送事件记录表")
public class AcmsEventRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "事件ID（外部系统事件唯一标识）")
    private String eventId;

    @ApiModelProperty(value = "事件类型：197162-人证比对，196893-人脸识别，198914-刷校园卡")
    private Integer eventType;

    @ApiModelProperty(value = "识别类型：人证比对/人脸识别/刷校园卡")
    private String recognitionType;

    @ApiModelProperty(value = "人员ID（ExtEventPersonNoj）")
    private String personId;

    @ApiModelProperty(value = "姓名")
    private String personName;

    @ApiModelProperty(value = "工号/学号")
    private String jobNo;

    @ApiModelProperty(value = "手机号")
    private String phoneNo;

    @ApiModelProperty(value = "性别：1-男，2-女，0-未知")
    private String gender;

    @ApiModelProperty(value = "身份证号")
    private String idCard;

    @ApiModelProperty(value = "所属单位")
    private String organization;

    @ApiModelProperty(value = "进出通道名称")
    private String channelName;

    @ApiModelProperty(value = "进出方向：进/出")
    private String direction;

    @ApiModelProperty(value = "车牌号码（只有访客会有）")
    private String plateNumber;

    @ApiModelProperty(value = "VIP类型名称（从访客预约记录中获取）")
    private String vipTypeName;

    @ApiModelProperty(value = "是否未预约纯访客：0-否（已预约或有预约记录），1-是（未预约的纯访客）。仅针对刷身份证进出（event_type=197162）的记录")
    private Integer isUnreservedVisitor;

    @ApiModelProperty(value = "预约时间段（格式：开始时间-结束时间），例如：2025-01-15 10:00:00-2025-01-15 12:00:00。仅针对已预约的访客")
    private String reservationTimeRange;

    @ApiModelProperty(value = "照片URL（人脸照片或身份证照片）")
    private String photoUrl;

    @ApiModelProperty(value = "事件发生时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date eventTime;

    @ApiModelProperty(value = "原始推送数据（JSON格式）")
    private String rawData;

    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @ApiModelProperty(value = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableLogic(value = "0", delval = "1")
    @ApiModelProperty(value = "逻辑删除：0-未删除，1-已删除")
    private Integer deleted;
}

