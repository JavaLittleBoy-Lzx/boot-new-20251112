package com.parkingmanage.vo;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import jdk.nashorn.internal.ir.annotations.Ignore;
import lombok.Data;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.Serializable;
import java.util.Date;

/**
 * @program: ParkManage
 * @description: 预约车辆分页查询参数实体类
 * @author: lzx
 * @create: 2023-11-14 11:14
 **/
@Data
public class VehcleReservationVo {

   @ApiModelProperty(value = "ID号")
   @TableId(value = "id", type = IdType.AUTO)
   private Integer id;

   @ApiModelProperty(value = "车场编码")
   private String yardCode;

   @ApiModelProperty(value = "车场名称")
   private String yardName;

   @ApiModelProperty(value = "入场通道")
   private String channelName;

   @ApiModelProperty(value = "车牌号码")
   private String plateNumber;

   @ApiModelProperty(value = "车辆分类")
   private String vehicleClassification;

   @ApiModelProperty(value = "商户名称")
   private String merchantName;

   @ApiModelProperty(value = "放行原因")
   private String releaseReason;

   @ApiModelProperty(value = "通知人姓名")
   @Ignore
   private String notifierName;

   @ApiModelProperty(value = "入场时间")
   private String enterTime;

   @ApiModelProperty(value = "备注")
   private String remark;

   @ApiModelProperty(value = "预约时间")
   @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
   private Date appointmentTime;

}
