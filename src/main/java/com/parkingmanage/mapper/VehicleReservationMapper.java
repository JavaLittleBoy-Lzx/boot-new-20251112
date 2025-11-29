package com.parkingmanage.mapper;

import cn.hutool.core.date.DateTime;
import com.parkingmanage.entity.ReportCarInData;
import com.parkingmanage.entity.VehicleReservation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author 李子雄
 */
public interface VehicleReservationMapper extends BaseMapper<VehicleReservation> {
    int duplicate(VehicleReservation vehicleReservation);

    List<ReportCarInData> findByLicenseNumber(String enterCarLicenseNumber);

    int updateEnterTime(String enterCarLicenseNumber, DateTime parse);

    int updateByCarNumber(String carNumber, String reserveTime, String enterTime, String enterVipType);

    int updateEnterVipType(String enterCarLicenseNumber, int enterVipType);

    int countByDate(String startDate, String endDate, String yardName);

    int countByVIPOutIndex(String startDate, String endDate, String yardName);

    int countByLinShiOutIndex(String startDate, String endDate, String yardName);

    List<VehicleReservation> queryListVehicleReservationExportLinShi(String startDate, String endDate, String yardName);

    List<VehicleReservation> queryListVehicleReservationExport(String carNo, String startDate, String endDate, String yardName);

    VehicleReservation selectByCarName(String enterCarLicenseNumber);

    VehicleReservation selectVehicleReservation(String enterCarLicenseNumber, String yardCode);
}
