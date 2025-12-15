package com.parkingmanage.service.impl;

import com.parkingmanage.entity.ReportCarIn;
import com.parkingmanage.entity.ReportCarInReservation;
import com.parkingmanage.entity.ReportCarOutReservation;
import com.parkingmanage.mapper.ReportCarInMapper;
import com.parkingmanage.service.ReportCarInService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author lzx
 * @since 2024-04-27
 */
@Service
public class ReportCarInServiceImpl extends ServiceImpl<ReportCarInMapper, ReportCarIn> implements ReportCarInService {

    @Override
    public boolean save(ReportCarIn entity) {
        return super.save(entity);
    }

    @Override
    public List<ReportCarIn> findByLicenseNumber(String parkingCode) {
        return Collections.emptyList();
    }

    @Override
    public int countByDate(String startDate, String endDate, String yardName) {
        return 0;
    }

    @Override
    public int countByDateVIP(String startDate, String endDate, String yardName) {
        return 0;
    }

    @Override
    public int updateByCarNumber(String carLicenseNumber, String preVipType) {
        return 0;
    }

    @Override
    public List<ReportCarInReservation> queryListReportOutExport(String startDate, String endDate, String yardName) {
        return Collections.emptyList();
    }

    @Override
    public List<ReportCarInReservation> queryListReportCarOutExportLinShi(String startDate, String endDate, String yardName) {
        return Collections.emptyList();
    }

    @Override
    public List<ReportCarIn> selectCarRecords(String carCode, String enterTime) {
        return Collections.emptyList();
    }

    @Override
    public List<ReportCarInReservation> queryListReportOutExportWan(String startDate, String endDate, String yardName, String channelName) {
        return Collections.emptyList();
    }

    @Override
    public List<ReportCarInReservation> queryListReportCarOutExportLinShiWan(String startDate, String endDate, String yardName, String channelName) {
        return Collections.emptyList();
    }

    @Override
    public int countByDateWan(String startDate, String endDate, String yardName, String channelName) {
        return 0;
    }

    @Override
    public int countByDateVIPWan(String startDate, String endDate, String yardName, String channelName) {
        return 0;
    }
}
