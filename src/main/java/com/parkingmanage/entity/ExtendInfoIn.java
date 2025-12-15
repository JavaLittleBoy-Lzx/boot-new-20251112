package com.parkingmanage.entity;

import lombok.Data;

/**
 * @program: ParkManage
 * @description:
 * @author: lzx
 * @create: 2024-04-26 08:55
 **/
@Data
public class ExtendInfoIn {
    private String enterNoVipCodeName;
    private String selfInCode;
    private String selfInUserUniqCode;
    private String inEtcReliable;
    private String inEtcCarCode;
    private String inEtcCode;
}