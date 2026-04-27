package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * 夜间学生出校提醒配置实体
 *
 * @author System
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("night_student_alert_config")
public class NightStudentAlertConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 是否启用：0-禁用，1-启用
     */
    private Integer enabled;

    /**
     * 夜间开始时间（如22:00）
     */
    private String nightStartTime;

    /**
     * 夜间结束时间（如06:00），支持跨天
     */
    private String nightEndTime;

    /**
     * 需要提醒的出口通道（逗号分隔，为空则全部通道）
     */
    private String alertChannels;

    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;
}
