package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
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
 * @since 2024-08-19
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="ExportData对象", description="")
public class ExportData implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "车牌号码")
    private String enterCarLicenseNumber;

    @ApiModelProperty(value = "通知人姓名")
    private String notifierName;

    @ApiModelProperty(value = "放行原因")
    private String reserveRemark;

    @ApiModelProperty(value = "入场时间")
    private LocalDateTime enterTime;

    @ApiModelProperty(value = "离场时间")
    private LocalDateTime leaveTime;

    @ApiModelProperty(value = "停车时长 xx天xx小时xx分钟xx秒")
    private String parkingDuration;

    @ApiModelProperty(value = "备注 超位的原因")
    private String remark;

    @ApiModelProperty(value = "进场通道")
    private String enterChannelName;
}