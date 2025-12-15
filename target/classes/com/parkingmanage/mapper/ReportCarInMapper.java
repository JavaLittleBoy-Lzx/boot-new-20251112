package com.parkingmanage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parkingmanage.entity.ReportCarIn;
import com.parkingmanage.entity.ReportCarInReservation;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author lzx
 * @since 2024-04-27
 */
public interface ReportCarInMapper extends BaseMapper<ReportCarIn> {

    List<ReportCarIn> findByLicenseNumber(String parkingCode);

    int countByDate(String startDate, String endDate, String yardName);

    int countByDateVIP(String startDate, String endDate, String yardName);

    int updateByCarNumber(String carLicenseNumber, String preVipType);

    List<ReportCarInReservation> queryListReportOutExport(String startDate, String endDate, String yardName);

    List<ReportCarInReservation> queryListReportCarOutExportLinShi(String startDate, String endDate, String yardName);

    List<ReportCarIn> selectCarRecords(String carCode,String enterTime);

    List<ReportCarInReservation> queryListReportOutExportWan(String startDate, String endDate, String yardName, String channelName);

    List<ReportCarInReservation> queryListReportCarOutExportLinShiWan(String startDate, String endDate, String yardName, String channelName);

    int countByDateWan(String startDate, String endDate, String yardName, String channelName);

    int countByDateVIPWan(String startDate, String endDate, String yardName, String channelName);
}
