package com.parkingmanage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parkingmanage.entity.FocusWatchList;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 关注监控列表Mapper
 * 
 * @author System
 */
@Mapper
public interface FocusWatchListMapper extends BaseMapper<FocusWatchList> {

    /**
     * 检查是否有用户关注该对象
     * 
     * @param watchType 关注类型
     * @param watchValue 关注值
     * @return 关注该对象的用户ID列表
     */
    @Select("SELECT user_id FROM focus_watch_list WHERE watch_type = #{watchType} AND watch_value = #{watchValue}")
    List<Integer> findMatchedUsers(@Param("watchType") String watchType, @Param("watchValue") String watchValue);

    /**
     * 查询用户关注的某个对象
     * 
     * @param userId 用户ID
     * @param watchType 关注类型
     * @param watchValue 关注值
     * @return 关注对象
     */
    @Select("SELECT * FROM focus_watch_list WHERE user_id = #{userId} AND watch_type = #{watchType} AND watch_value = #{watchValue}")
    FocusWatchList findByUserAndValue(@Param("userId") Integer userId, 
                                      @Param("watchType") String watchType, 
                                      @Param("watchValue") String watchValue);
}
