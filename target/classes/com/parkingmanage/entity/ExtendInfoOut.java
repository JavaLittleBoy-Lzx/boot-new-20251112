package com.parkingmanage.entity;

import lombok.Data;

/**
 * @program: ParkManage
 * @description:
 * @author: lzx
 * @create: 2024-04-27 16:25
 **/
@Data
public class ExtendInfoOut {
    private String enterNoVipCodeName;
    private String leaveNoVipCodeName;
    private int leaveCarModel;
    private String selfInCode;
    private String selfOutCode;
    private String selfInUserUniqCode;
    private String selfOutUserUniqCode;
}