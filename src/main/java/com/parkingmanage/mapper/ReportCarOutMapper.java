package com.parkingmanage.mapper;

import com.parkingmanage.entity.ReportCarOut;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parkingmanage.entity.ReportCarOutReservation;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author lzx
 * @since 2024-04-27
 */
public interface ReportCarOutMapper extends BaseMapper<ReportCarOut> {

    List<ReportCarOut> findByLicenseNumber(String parkingCode);

    List<ReportCarOutReservation> queryListReportCarOutExportLinShi(String startDate, String endDate, String yardName);

    List<ReportCarOutReservation> queryListReportOutExport(String startDate, String endDate, String yardName);

    List<ReportCarOut> selectLeaveTime(String enterCarLicenseNumber, String enterTime, String startDate, String endDate);

    List<ReportCarOut> selectCarRecords(String carCode,String enterTime);
}
