package com.parkingmanage.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.List;

/**
 * @program: ParkManage
 * @description: 参数实体类
 * @author: lzx
 * @create: 2023-11-04 14:27
 **/
@Data
public class ReportPreCarIn {
//    private String cmd;
//    private Integer enterCarLicenseColor;
//    private String enterCarLicenseNumber;
//    private Integer enterCarLicenseType;
//    private String enterChannelCode;
//    private String enterChannelCustomCode;
//    private Integer enterChannelId;
//    private String channelName;
//    private String enterChargeGroupCode;
//    private String enterTime;
//    @JsonIgnore
//    private String enterImageArray;
//    private String lockKey;
//    private String parkCode;
    private int enterCarType;
    private String enterChannelCustomCode;
    private int enterCarLicenseColor;
    private String enterChannelCode;
    private String enterCarLicenseNumber;
    private List<EnterImageArray> enterImageArray;
    private String cmd;
    private int enterChannelId;
    private String lockKey;
    private String parkCode;
    private int enterCarLicenseType;
    private String enterChargeGroupCode;
    private String enterTime;
}