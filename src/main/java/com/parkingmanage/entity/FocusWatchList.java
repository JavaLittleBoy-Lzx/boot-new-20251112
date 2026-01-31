package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * 关注监控列表实体
 * 
 * @author System
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("focus_watch_list")
public class FocusWatchList implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Integer userId;

    private String watchType;

    private String watchValue;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;
}
