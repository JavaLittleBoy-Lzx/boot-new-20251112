package com.parkingmanage.utils;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.api.R;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class ResultUtil {

    /**
     * 构建分页查询的返回结果
     *
     * @param page
     * @return
     */

    public static R<Map<String, Object>> buildPageR(IPage<?> page) {
        HashMap<String, Object> data = new HashMap<>();
        data.put("count", page.getTotal());
        data.put("records", page.getRecords());
        return R.ok(data);
    }

    /**
     * 成功或失败的响应信息
     *
     * @param successs
     * @return
     */
    public static R<Object> buildR(boolean successs) {
        if (successs) {
            return R.ok(null);
        }
        return R.failed("操作失败");
    }

    public static String convertDate(String strDate) {
        String str = "";
        
        // 检查输入是否为null、空字符串或"null"字符串
        if (strDate == null || strDate.trim().isEmpty() || "null".equals(strDate)) {
            return str;
        }
        
        if (strDate.length() == 16) {
//            System.out.println("Str Object is String"); length = 16
            strDate = strDate + ":00";
        }
        try {
            String fmt = "yyyy-MM-dd HH:mm:ss";
            strDate = strDate.replace("T", " ");
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(fmt);
            return dateTimeFormatter.format(dateTimeFormatter.parse(strDate));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return str;
    }
}
