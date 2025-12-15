package com.parkingmanage.utils;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.api.R;
import java.util.HashMap;
import java.util.Map;
public class ResourceUtil {
    public static  R<Map<String,Object>> buildPageR(IPage<?> page){
        HashMap<String, Object> map = new HashMap<>();
        map.put("count",page.getTotal());
        map.put("records",page.getRecords());
        return R.ok(map);
    }
    public static R<Object> buildR(boolean success){
        if(success){
            return R.ok(null);
        }
        return R.failed("操作失败！");
    }
}