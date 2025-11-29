package com.parkingmanage.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 模拟预约请求参数
 * 
 * @author System
 */
@Data
@ApiModel(value="MockAppointmentRequest", description="模拟预约请求参数")
public class MockAppointmentRequest {
    
    @ApiModelProperty(value = "微信OpenID")
    private String openid;
    
    @ApiModelProperty(value = "车牌号")
    private String plateNumber;
    
    @ApiModelProperty(value = "停车场名称")
    private String yardName;
} 