package com.parkingmanage.dto;

import lombok.Data;
import java.util.List;

/**
 * 搜索结果封装类
 */
@Data
public class SearchResult<T> {
    
    /**
     * 搜索结果列表
     */
    private List<T> records;
    
    /**
     * 总记录数
     */
    private Long total;
    
    /**
     * 当前页码
     */
    private Integer current;
    
    /**
     * 每页大小
     */
    private Integer size;
    
    /**
     * 搜索耗时(毫秒)
     */
    private Long searchTime;
    
    /**
     * 搜索关键词
     */
    private String keyword;
    
    /**
     * 是否还有更多数据
     */
    private Boolean hasMore;
    
    /**
     * 构造方法
     */
    public SearchResult() {}
    
    public SearchResult(List<T> records, Long total, Integer current, Integer size) {
        this.records = records;
        this.total = total;
        this.current = current;
        this.size = size;
        this.hasMore = (current * size) < total;
    }
    
    public SearchResult(List<T> records, Long total, Integer current, Integer size, Long searchTime, String keyword) {
        this(records, total, current, size);
        this.searchTime = searchTime;
        this.keyword = keyword;
    }
} 