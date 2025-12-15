package com.parkingmanage.service.impl;

import cn.hutool.core.date.DateTime;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.parkingmanage.entity.ReportCarInData;
import com.parkingmanage.entity.VehicleReservation;
import com.parkingmanage.mapper.VehicleReservationMapper;
import com.parkingmanage.service.ReportCarInService;
import com.parkingmanage.service.ReportCarOutService;
import com.parkingmanage.service.VehicleReservationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.List;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 李子雄
 */
@Slf4j
@Service
public class VehicleReservationServiceImpl extends ServiceImpl<VehicleReservationMapper, VehicleReservation> implements VehicleReservationService {

    @Resource
    private VehicleReservationService vehicleReservationService;

    @Resource
    private ReportCarInService reportCarInService;

    @Resource
    private ReportCarOutService reportCarOutService;


    @Override
    public int duplicate(VehicleReservation vehicleReservation) {
        return baseMapper.duplicate(vehicleReservation);
    }

    @Override
    public List<VehicleReservation> queryListVehicleReservation(String plateNumber, String yardName) {
        LambdaQueryWrapper<VehicleReservation> queryWrapper = new LambdaQueryWrapper();
        if (StringUtils.hasLength(plateNumber)) {
            queryWrapper.like(VehicleReservation::getPlateNumber, plateNumber);
        }
        if (StringUtils.hasLength(yardName)) {
            queryWrapper.like(VehicleReservation::getYardName, yardName);
        }
        queryWrapper.eq(VehicleReservation::getAppointmentFlag, 0);
        List<VehicleReservation> vehicleReservations = vehicleReservationService.list(queryWrapper);
        return vehicleReservations;
    }

    @Override
    public List<VehicleReservation> queryListVehicleReservationSuccess(String plateNumber, String yardName) {
        LambdaQueryWrapper<VehicleReservation> queryWrapper = new LambdaQueryWrapper();
        if (StringUtils.hasLength(plateNumber)) {
            queryWrapper.like(VehicleReservation::getPlateNumber, plateNumber);
        }
        if (StringUtils.hasLength(yardName)) {
            queryWrapper.like(VehicleReservation::getYardName, yardName);
        }

        queryWrapper.eq(VehicleReservation::getReserveFlag, 1);
        List<VehicleReservation> vehicleReservations = vehicleReservationService.list(queryWrapper);
        return vehicleReservations;
    }

    @Override
    public void exportVehicleReservation(String startDate, String endDate, String yardName, String channelName, HttpServletResponse response) throws IOException, ParseException {

    }

    public static String calculatePercentage(double num1, double num2) {
        double result = (num1 / num2) * 100;
        DecimalFormat df = new DecimalFormat("#.00");
        return df.format(result);
    }


    @Override
    public List<VehicleReservation> queryListVehicleReservationExport(String carNo, String startDate, String endDate, String yardName) {
        return baseMapper.queryListVehicleReservationExport(carNo, startDate, endDate, yardName);
    }

    @Override
    public List<ReportCarInData> findByLicenseNumber(String enterCarLicenseNumber) {
        return baseMapper.findByLicenseNumber(enterCarLicenseNumber);
    }

    @Override
    public int updateEnterTime(String enterCarLicenseNumber, DateTime parse) {

        return baseMapper.updateEnterTime(enterCarLicenseNumber, parse);
    }

    @Override
    public int updateByCarNumber(String carNumber, String reserveTime, String enterTime, String enterVipType) {
        return baseMapper.updateByCarNumber(carNumber, reserveTime, enterTime, enterVipType);
    }

    @Override
    public int updateEnterVipType(String enterCarLicenseNumber, int enterVipType) {
        return baseMapper.updateEnterVipType(enterCarLicenseNumber, enterVipType);
    }

    @Override
    public int countByDate(String startDate, String endDate, String yardName) {
        return baseMapper.countByDate(startDate, endDate, yardName);
    }

    @Override
    public int countByVIPOutIndex(String startDate, String endDate, String yardName) {
        return baseMapper.countByVIPOutIndex(startDate, endDate, yardName);
    }

    @Override
    public int countByLinShiOutIndex(String startDate, String endDate, String yardName) {
        return baseMapper.countByLinShiOutIndex(startDate, endDate, yardName);
    }

    @Override
    public List<VehicleReservation> queryListVehicleReservationExportLinShi(String startDate, String endDate, String yardName) {
        return baseMapper.queryListVehicleReservationExportLinShi(startDate, endDate, yardName);
    }

    @Override
    public VehicleReservation selectByCarName(String enterCarLicenseNumber) {
        return baseMapper.selectByCarName(enterCarLicenseNumber);
    }

    @Override
    public int batchDelete(List<Integer> ids) {
        return baseMapper.deleteBatchIds(ids);
    }

    @Override
    public VehicleReservation selectVehicleReservation(String enterCarLicenseNumber, String yardCode) {
        return baseMapper.selectVehicleReservation(enterCarLicenseNumber, yardCode);
    }
}
