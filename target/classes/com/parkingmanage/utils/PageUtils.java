package com.parkingmanage.utils;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 @Description:手动分页工具类
 @PROJECT_NAME: parkingmanage
 @PACKAGE_NAME: com.parkingmanage.utils
 @NAME: PageUtils
 @author:yuli
 @Version: 1.0
 @DATE: 2021/11/10 16:29
*/
@Slf4j
public class PageUtils {
    public static List getPageList(List list, Integer pageNum, Integer pageSize) {
        if (CollectionUtils.isEmpty(list)) {
            return new ArrayList();
        }
        Integer count = list.size(); // 记录总数
        Integer pageCount = 0; // 页数
        if (count % pageSize == 0) {
            pageCount = count / pageSize;
        } else {
            pageCount = count / pageSize + 1;
        }
        int fromIndex = 0; // 开始索引
        int toIndex = 0; // 结束索引
        if (!pageNum.equals(pageCount)) {
            fromIndex = (pageNum - 1) * pageSize;
            toIndex = fromIndex + pageSize;
        } else {
            fromIndex = (pageNum - 1) * pageSize;
            toIndex = count;
        }
        List pageList = list.subList(fromIndex, toIndex);

        return pageList;
    }

    public static <E> Page<E> getPage(List<E> list, Integer pageNum, Integer pageSize) {

        Integer totalRow = CollectionUtils.isEmpty(list) ? 0 : list.size();
        Page<E> page = new Page<>();
        page.setCurrent(pageNum);
        page.setSize(pageSize);
        page.setTotal(totalRow);
        page.setPages((int) Math.ceil((float) totalRow / pageSize));
        page.setRecords(getPageList(list, pageNum, pageSize));
        return page;
    }
}