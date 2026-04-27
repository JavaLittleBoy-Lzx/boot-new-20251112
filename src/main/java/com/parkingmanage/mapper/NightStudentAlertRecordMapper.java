package com.parkingmanage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parkingmanage.entity.NightStudentAlertRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 夜间学生出校提醒记录Mapper
 *
 * @author System
 */
@Mapper
public interface NightStudentAlertRecordMapper extends BaseMapper<NightStudentAlertRecord> {

    /**
     * 获取未读记录数量
     *
     * @return 未读数量
     */
    @Select("SELECT COUNT(*) FROM night_student_alert_record WHERE is_read = 0")
    int countUnread();

    /**
     * 分页查询记录
     *
     * @param page 分页对象
     * @param channelName 通道名称（筛选）
     * @param gender 性别（筛选）
     * @param college 学院（筛选）
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 记录列表
     */
    @Select("<script>" +
            "SELECT * FROM night_student_alert_record WHERE 1=1 " +
            "<if test='channelName != null and channelName != \"\"'> AND channel_name = #{channelName}</if>" +
            "<if test='gender != null and gender != \"\"'> AND gender = #{gender}</if>" +
            "<if test='college != null and college != \"\"'> AND college LIKE CONCAT('%', #{college}, '%')</if>" +
            "<if test='startTime != null'> AND event_time &gt;= #{startTime}</if>" +
            "<if test='endTime != null'> AND DATE_ADD(#{endTime}, INTERVAL 1 DAY) &gt; event_time</if>" +
            " ORDER BY event_time DESC" +
            "</script>")
    IPage<NightStudentAlertRecord> selectByPage(Page<NightStudentAlertRecord> page,
                                                @Param("channelName") String channelName,
                                                @Param("gender") String gender,
                                                @Param("college") String college,
                                                @Param("startTime") Date startTime,
                                                @Param("endTime") Date endTime);

    /**
     * 按通道统计
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 通道统计数据
     */
    @Select("<script>" +
            "SELECT channel_name as channelName, COUNT(*) as count " +
            "FROM night_student_alert_record WHERE 1=1 " +
            "<if test='startTime != null'> AND event_time &gt;= #{startTime}</if>" +
            "<if test='endTime != null'> AND DATE_ADD(#{endTime}, INTERVAL 1 DAY) &gt; event_time</if>" +
            " GROUP BY channel_name ORDER BY count DESC" +
            "</script>")
    List<Map<String, Object>> selectGroupByChannel(@Param("startTime") Date startTime,
                                                    @Param("endTime") Date endTime);

    /**
     * 按性别统计
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 性别统计数据
     */
    @Select("<script>" +
            "SELECT gender as gender, COUNT(*) as count " +
            "FROM night_student_alert_record WHERE 1=1 " +
            "<if test='startTime != null'> AND event_time &gt;= #{startTime}</if>" +
            "<if test='endTime != null'> AND DATE_ADD(#{endTime}, INTERVAL 1 DAY) &gt; event_time</if>" +
            " GROUP BY gender ORDER BY count DESC" +
            "</script>")
    List<Map<String, Object>> selectGroupByGender(@Param("startTime") Date startTime,
                                                   @Param("endTime") Date endTime);

    /**
     * 按学院统计
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 学院统计数据
     */
    @Select("<script>" +
            "SELECT college as college, COUNT(*) as count " +
            "FROM night_student_alert_record WHERE 1=1 " +
            "<if test='startTime != null'> AND event_time &gt;= #{startTime}</if>" +
            "<if test='endTime != null'> AND DATE_ADD(#{endTime}, INTERVAL 1 DAY) &gt; event_time</if>" +
            " GROUP BY college ORDER BY count DESC" +
            "</script>")
    List<Map<String, Object>> selectGroupByCollege(@Param("startTime") Date startTime,
                                                   @Param("endTime") Date endTime);

    /**
     * 按小时统计（时段分布）
     */
    @Select("<script>" +
            "SELECT HOUR(event_time) as hour, COUNT(*) as count " +
            "FROM night_student_alert_record WHERE 1=1 " +
            "<if test='startTime != null'> AND event_time &gt;= #{startTime}</if>" +
            "<if test='endTime != null'> AND DATE_ADD(#{endTime}, INTERVAL 1 DAY) &gt; event_time</if>" +
            " GROUP BY HOUR(event_time) ORDER BY hour" +
            "</script>")
    List<Map<String, Object>> selectGroupByHour(@Param("startTime") Date startTime,
                                                 @Param("endTime") Date endTime);

    /**
     * 按日期统计（日出校趋势）
     */
    @Select("<script>" +
            "SELECT DATE_FORMAT(event_time, '%Y-%m-%d') as date, COUNT(*) as count " +
            "FROM night_student_alert_record WHERE 1=1 " +
            "<if test='startTime != null'> AND event_time &gt;= #{startTime}</if>" +
            "<if test='endTime != null'> AND DATE_ADD(#{endTime}, INTERVAL 1 DAY) &gt; event_time</if>" +
            " GROUP BY DATE_FORMAT(event_time, '%Y-%m-%d') ORDER BY date" +
            "</script>")
    List<Map<String, Object>> selectGroupByDay(@Param("startTime") Date startTime,
                                                @Param("endTime") Date endTime);

    /**
     * 从night_student_alert_record表获取所有不重复的出口通道名称
     *
     * @return 通道名称列表
     */
    @Select("SELECT DISTINCT channel_name FROM night_student_alert_record WHERE channel_name IS NOT NULL AND channel_name != '' AND channel_name LIKE '%出口%' ORDER BY channel_name")
    List<String> selectAllChannelNames();

    /**
     * 从night_student_alert_record表获取所有不重复的学院名称
     *
     * @return 学院名称列表
     */
    @Select("SELECT DISTINCT college FROM night_student_alert_record WHERE college IS NOT NULL AND college != '' ORDER BY college")
    List<String> selectAllColleges();
}
