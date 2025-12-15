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
 * 违规车辆记录实体
 * 用于存储从ACMS/海康系统推送的违规车辆数据
 * 
 * @author System
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("violation_vehicle_record")
@ApiModel(value = "ViolationVehicleRecord对象", description = "违规车辆记录表")
public class ViolationVehicleRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "事件ID（外部系统事件唯一标识）")
    private String eventId;

    @ApiModelProperty(value = "事件类型代码")
    private Integer eventType;

    @ApiModelProperty(value = "事件类型名称")
    private String eventTypeName;

    @ApiModelProperty(value = "车牌号码")
    private String plateNo;

    @ApiModelProperty(value = "车牌颜色：1-蓝色，2-黄色，3-黑色，4-白色，5-绿色")
    private Integer plateColor;

    @ApiModelProperty(value = "车牌颜色名称")
    private String plateColorName;

    @ApiModelProperty(value = "违规类型代码")
    private String violationType;

    @ApiModelProperty(value = "违规类型名称")
    private String violationTypeName;

    @ApiModelProperty(value = "违规地点/卡口名称")
    private String location;

    @ApiModelProperty(value = "车道号")
    private Integer laneNo;

    @ApiModelProperty(value = "车辆类型：1-小型车，2-大型车，3-摩托车等")
    private Integer vehicleType;

    @ApiModelProperty(value = "车辆类型名称")
    private String vehicleTypeName;

    @ApiModelProperty(value = "车辆颜色")
    private String vehicleColor;

    @ApiModelProperty(value = "车速（km/h）")
    private Integer speed;

    @ApiModelProperty(value = "限速（km/h）")
    private Integer speedLimit;

    @ApiModelProperty(value = "车辆方向：0-入场，1-出场")
    private Integer direction;

    @ApiModelProperty(value = "车辆方向名称：进/出")
    private String directionName;

    @ApiModelProperty(value = "车辆图片URL")
    private String vehiclePictureUrl;

    @ApiModelProperty(value = "车牌图片URL")
    private String platePictureUrl;

    @ApiModelProperty(value = "全景图片URL")
    private String panoramaPictureUrl;

    @ApiModelProperty(value = "事件发生时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date happenTime;

    @ApiModelProperty(value = "处理状态：0-未处理，1-已处理，2-已忽略")
    private Integer processStatus;

    @ApiModelProperty(value = "处理时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date processTime;

    @ApiModelProperty(value = "处理人")
    private String processUser;

    @ApiModelProperty(value = "处理备注")
    private String processRemark;

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
