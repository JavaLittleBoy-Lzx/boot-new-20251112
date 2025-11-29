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
 * 访客预约同步实体
 * 用于存储从外部接口同步的访客预约数据
 * 
 * @author System
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("visitor_reservation_sync")
@ApiModel(value = "VisitorReservationSync对象", description = "访客预约同步表")
public class VisitorReservationSync implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "预约记录ID（外部系统）")
    private String reservationId;

    @ApiModelProperty(value = "被访部门名称")
    private String passDep;

    @ApiModelProperty(value = "访客姓名")
    private String visitorName;
    
    @ApiModelProperty(value = "访客手机号码")
    private String visitorPhone;

    @ApiModelProperty(value = "访客身份证号码")
    private String visitorIdCard;

    @ApiModelProperty(value = "随行车辆")
    private String carNumber;

    @ApiModelProperty(value = "预约开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;

    @ApiModelProperty(value = "预约结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime;

    @ApiModelProperty(value = "网关通行开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date gatewayTransitBeginTime;

    @ApiModelProperty(value = "网关通行结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date gatewayTransitEndTime;

    @ApiModelProperty(value = "预约VIP类型")
    private String vipTypeName;

    @ApiModelProperty(value = "车场名称")
    private String parkName;

    @ApiModelProperty(value = "被访人")
    private String passName;

    @ApiModelProperty(value = "申请状态名称")
    private String applyStateName;

    @ApiModelProperty(value = "发起渠道")
    private String applyFromName;

    @ApiModelProperty(value = "备注信息1")
    private String remark1;

    @ApiModelProperty(value = "备注信息2")
    private String remark2;

    @ApiModelProperty(value = "备注信息3")
    private String remark3;

    @ApiModelProperty(value = "数据指纹（用于快速检测数据是否变化，避免逐字段对比）")
    private Integer dataHash;

    @ApiModelProperty(value = "VIP是否已开通：0-未开通，1-已开通")
    private Integer vipOpened;

    @ApiModelProperty(value = "VIP开通时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date vipOpenTime;

    @ApiModelProperty(value = "是否已调用接口添加访客：0-未调用，1-已调用")
    private Integer apiCalled;

    @ApiModelProperty(value = "调用接口添加访客的时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date apiCallTime;

    @ApiModelProperty(value = "外部系统创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date externalCreateTime;

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

    @ApiModelProperty(value = "人员来访状态：人未来访/人已进场/人已离场/来访中")
    private String personVisitStatus;

    @ApiModelProperty(value = "人员进出场时间记录，JSON格式")
    private String personVisitTimes;

    @ApiModelProperty(value = "车辆来访状态：车未来访/已进场/已离场")
    private String carVisitStatus;

    @ApiModelProperty(value = "车辆进出场时间记录，JSON格式")
    private String carVisitTimes;
}

