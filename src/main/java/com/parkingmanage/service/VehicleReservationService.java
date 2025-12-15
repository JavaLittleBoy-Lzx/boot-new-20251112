package com.parkingmanage.service;

import cn.hutool.core.date.DateTime;
import com.parkingmanage.entity.ReportCarInData;
import com.parkingmanage.entity.VehicleReservation;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;

/**
 <p>
  服务类
 </p>

 @author 李子雄

*/
public interface VehicleReservationService extends IService<VehicleReservation> {
    int duplicate(VehicleReservation vehicleReservation);
    List<VehicleReservation> queryListVehicleReservation(String plateNumber, String yardName);

    List<VehicleReservation> queryListVehicleReservationSuccess(String plateNumber, String yardName);

    void exportVehicleReservation(String startDate, String endDate, String yardName, String channelName,HttpServletResponse response) throws IOException, ParseException;

    List<VehicleReservation> queryListVehicleReservationExport(String carNo,String startDate, String endDate, String yardName);


    List<ReportCarInData> findByLicenseNumber(String enterCarLicenseNumber);

    int updateEnterTime(String enterCarLicenseNumber, DateTime parse);

    int updateByCarNumber(String carNumber,String reserveTime,String enterTime,String enterVipType);

    int updateEnterVipType(String enterCarLicenseNumber, int enterVipType);

    int countByDate(String startDate, String endDate, String yardName);

    int countByVIPOutIndex(String startDate, String endDate, String yardName);

    int countByLinShiOutIndex(String startDate, String endDate, String yardName);

    List<VehicleReservation> queryListVehicleReservationExportLinShi(String startDate, String endDate, String yardName);

    VehicleReservation selectByCarName(String enterCarLicenseNumber);

    int batchDelete(List<Integer> ids);

    VehicleReservation selectVehicleReservation(String enterCarLicenseNumber, String yardCode);
}
