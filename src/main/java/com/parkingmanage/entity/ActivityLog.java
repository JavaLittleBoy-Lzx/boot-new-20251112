package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 活动日志表
 * </p>
 *
 * @author System
 * @since 2024-12-19
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "ActivityLog对象", description = "活动日志记录")
public class ActivityLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "用户ID")
    @TableField("user_id")
    private String userId;

    @ApiModelProperty(value = "用户名")
    @TableField("username")
    private String username;

    @ApiModelProperty(value = "操作模块")
    @TableField("module")
    private String module;

    @ApiModelProperty(value = "操作动作")
    @TableField("action")
    private String action;

    @ApiModelProperty(value = "操作描述")
    @TableField("description")
    private String description;

    @ApiModelProperty(value = "操作对象ID")
    @TableField("target_id")
    private String targetId;

    @ApiModelProperty(value = "操作对象类型")
    @TableField("target_type")
    private String targetType;

    @ApiModelProperty(value = "操作前数据")
    @TableField("old_data")
    private String oldData;

    @ApiModelProperty(value = "操作后数据")
    @TableField("new_data")
    private String newData;

    @ApiModelProperty(value = "IP地址")
    @TableField("ip_address")
    private String ipAddress;

    @ApiModelProperty(value = "浏览器信息")
    @TableField("user_agent")
    private String userAgent;

    @ApiModelProperty(value = "操作状态：success-成功，failed-失败")
    @TableField("status")
    private String status;

    @ApiModelProperty(value = "错误信息")
    @TableField("error_message")
    private String errorMessage;

    @ApiModelProperty(value = "操作时间")
    @TableField("created_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "操作耗时(毫秒)")
    @TableField("duration")
    private Long duration;

    @ApiModelProperty(value = "备注")
    @TableField("remark")
    private String remark;
} 