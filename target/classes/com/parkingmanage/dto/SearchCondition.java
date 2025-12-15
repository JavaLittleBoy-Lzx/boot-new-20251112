package com.parkingmanage.dto;

import lombok.Data;

/**
 * 搜索条件封装类
 */
@Data
public class SearchCondition {
    
    /**
     * 搜索关键词
     */
    private String keyword;
    
    /**
     * 车场代码
     */
    private String parkCode;
    
    /**
     * 车场名称
     */
    private String parkName;
    
    /**
     * 是否只返回在场车辆
     */
    private Boolean onlyInPark;
    
    /**
     * 搜索类型
     */
    private SearchType searchType;
    
    /**
     * 页码
     */
    private Integer page;
    
    /**
     * 每页大小
     */
    private Integer size;
    
    /**
     * 搜索类型枚举
     */
    public enum SearchType {
        PLATE_NUMBER,    // 车牌号搜索
        PHONE_NUMBER,    // 手机号搜索
        OWNER_NAME,      // 车主姓名搜索
        MIXED           // 混合搜索
    }
} 