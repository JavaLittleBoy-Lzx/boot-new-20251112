package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * 关注提醒记录实体
 * 
 * @author System
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("focus_alert_records")
public class FocusAlertRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Integer userId;

    private Long watchId;

    private String alertType;

    private String watchValue;

    private String eventType;

    private Date eventTime;

    private String channelName;

    private String personName;

    private String department;

    private String phoneNo;

    private String photoUrl;

    private String enterChannelName;

    private String stoppingTime;

    private String reservationPerson;

    private String reservationPhone;

    private String reservationReason;

    private String reservationTimeRange;

    private String visitorPassName;

    private String visitorVipType;

    private String visitorParkName;

    private String visitorReservationTimeRange;

    private Integer isConfirmed;

    private Date confirmedAt;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;
}
