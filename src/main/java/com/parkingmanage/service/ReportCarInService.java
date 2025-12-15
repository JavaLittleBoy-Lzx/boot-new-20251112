package com.parkingmanage.service;

import com.parkingmanage.entity.ReportCarIn;
import com.baomidou.mybatisplus.extension.service.IService;
import com.parkingmanage.entity.ReportCarInReservation;
import com.parkingmanage.entity.ReportCarOutReservation;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author lzx
 * @since 2024-04-27
 */
public interface ReportCarInService extends IService<ReportCarIn> {

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
