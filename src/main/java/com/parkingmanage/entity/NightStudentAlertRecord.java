package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * 夜间学生出校提醒记录实体
 *
 * @author System
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("night_student_alert_record")
public class NightStudentAlertRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 学生姓名
     */
    private String personName;

    /**
     * 身份证号（脱敏存储）
     */
    private String idCard;

    /**
     * 学号
     */
    private String jobNo;

    /**
     * 性别：男/女
     */
    private String gender;

    /**
     * 学院/部门
     */
    private String college;

    /**
     * 出校通道名称
     */
    private String channelName;

    /**
     * 出校时间（存储原始字符串，不转换时区）
     */
    private String eventTime;

    /**
     * 照片URL
     */
    private String photoUrl;

    /**
     * 是否已读：0-未读，1-已读
     */
    private Integer isRead;

    /**
     * 已读时间
     */
    private Date readAt;

    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;
}
