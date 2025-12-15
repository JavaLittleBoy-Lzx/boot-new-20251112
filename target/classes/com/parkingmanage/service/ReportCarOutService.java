package com.parkingmanage.service;

import com.parkingmanage.entity.ReportCarOut;
import com.baomidou.mybatisplus.extension.service.IService;
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
public interface ReportCarOutService extends IService<ReportCarOut> {

    List<ReportCarOut> findByLicenseNumber(String parkingCode);

    List<ReportCarOutReservation> queryListReportOutExport(String startDate, String endDate, String yardName);

    List<ReportCarOutReservation> queryListReportCarOutExportLinShi(String startDate, String endDate, String yardName);

    List<ReportCarOut> selectLeaveTime(String enterCarLicenseNumber,String enterTime,String startDate,String endDate);

    List<ReportCarOut> selectCarRecords(String carCode,String enterTime);
}
