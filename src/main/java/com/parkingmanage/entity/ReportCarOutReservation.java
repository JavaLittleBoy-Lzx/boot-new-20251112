package com.parkingmanage.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * <p>
 * 
 * </p>
 *
 * @author lzx
 * @since 2024-04-27
 */
@Data
//@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="ReportCarOut对象", description="")
public class ReportCarOutReservation implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "出场通道名，路边停车场没有")
    private String leaveChannelName;

    @ApiModelProperty(value = "离场时间")
    private String leaveTime;

    @ApiModelProperty(value = "出场类型，（参考附录进出场类型）")
    private String leaveType;

    @ApiModelProperty(value = "进场通道编号")
    private String enterChannelCode;

    @ApiModelProperty(value = "进场类型（参考附录）")
    private String enterType;

    @ApiModelProperty(value = "出场Vip类型（参考附录VIP类型）")
    private String leaveVipType;

    @ApiModelProperty(value = "进场Vip类型")
    private String enterVipType;

    @ApiModelProperty(value = "最终车牌号")
    private String carLicenseNumber;

    @ApiModelProperty(value = "车辆类型（0未知，1蓝牌车，2黄牌车，3超大型车 4新能源小车，5新能源大车（vems：20 非机动车））")
    private String enterCarType;

    @ApiModelProperty(value = "停车费用")
    private String totalAmount;

    @ApiModelProperty(value = "出场说明（参考附录）")
    private String leaveNoVipCodeName;

    @ApiModelProperty(value = "进场说明（参考附录）")
    private String enterNoVipCode;

    @ApiModelProperty(value = "车牌号")
    private String enterCarLicenseNumber;

    @ApiModelProperty(value = "离场车牌")
    private String leaveCarLicenseNumber;

    @ApiModelProperty(value = "进场说明（参考附录）")
    private String enterNoVipCodeName;

    @ApiModelProperty(value = "停车记录")
    private String parkingCode;

    @ApiModelProperty(value = "车场名称")
    private String yardName;

    @ApiModelProperty(value = "进场通道名称")
    private String enterChannelName;

    @ApiModelProperty(value = "入场时间")
    private String enterTime;

    @ApiModelProperty(value = "入场车牌颜色")
    private String enterCarLicenseColor;

    @ApiModelProperty(value = "出场车牌颜色（0未定义，1蓝色，2黄色，3白色，4黑色，5绿色）")
    private String leaveCarLicenseColor;

    @ApiModelProperty(value = "放出操作员名")
    private String outOperatorName;

    @ApiModelProperty(value = "进场通道id")
    private String enterChannelId;

    @ApiModelProperty(value = "进场放行操作员")
    private String inOperatorName;

    @ApiModelProperty(value = "进场放行时间")
    private String inOperatorTime;

    @ApiModelProperty(value = "车位id")
    private String areaId;
    @ApiModelProperty(value = "放行原因")
    private String releaseReason;

    @ApiModelProperty(value = "通知人姓名")
    private String notifierName;

    @ApiModelProperty(value = "备注")
    private String remark;

    @TableLogic(value="0",delval="1")
    @ApiModelProperty(value = "逻辑删除标识：0：未删除，1：已删除")
    private Integer deleted;

    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @ApiModelProperty(value = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date updateTime;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReportCarOutReservation)) return false;
        ReportCarOutReservation that = (ReportCarOutReservation) o;
        return Objects.equals(getParkingCode(), that.getParkingCode());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getParkingCode());
    }
}
