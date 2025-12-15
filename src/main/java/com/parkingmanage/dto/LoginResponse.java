package com.parkingmanage.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Map;

/**
 * 登录响应DTO
 * 
 * @author parking-system
 * @version 1.0
 */
@Data
@ApiModel(value = "LoginResponse", description = "登录响应对象")
public class LoginResponse {

    @ApiModelProperty(value = "访问令牌", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;

    @ApiModelProperty(value = "令牌类型", example = "Bearer")
    private String tokenType = "Bearer";

    @ApiModelProperty(value = "令牌过期时间（秒）", example = "86400")
    private Long expiresIn;

    @ApiModelProperty(value = "用户信息")
    private Map<String, Object> user;

    @ApiModelProperty(value = "刷新令牌", example = "refresh_token_example")
    private String refreshToken;
} 