package com.parkingmanage.service.impl;

import com.parkingmanage.entity.ReportCarOut;
import com.parkingmanage.entity.ReportCarOutReservation;
import com.parkingmanage.mapper.ReportCarOutMapper;
import com.parkingmanage.service.ReportCarOutService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.parkingmanage.service.VehicleReservationService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author lzx
 * @since 2024-04-27
 */
@Service
public class ReportCarOutServiceImpl extends ServiceImpl<ReportCarOutMapper, ReportCarOut> implements ReportCarOutService {
    @Resource
    private ReportCarOutService reportCarOutService;

    @Resource
    private VehicleReservationService vehicleReservationService;

    @Override
    public List<ReportCarOut> findByLicenseNumber(String parkingCode) {
        return baseMapper.findByLicenseNumber(parkingCode);
    }

    @Override
    public List<ReportCarOutReservation> queryListReportOutExport(String startDate, String endDate, String yardName) {
         // 连接ReportCarOut和
//        ArrayList<ReportCarOutReservation> reportCarOutReservations = new ArrayList<>();
//        ReportCarOutReservation reportCarOutReservation = new ReportCarOutReservation();
//        // 先在reportCarOut表中查询出符合条件的值
//        // lambdaQuery查询不相等数据
//        List<ReportCarOut> reportCarOutList = reportCarOutService.lambdaQuery().eq(ReportCarOut::getYardName, yardName).between(ReportCarOut::getEnterTime, startDate, endDate).eq(ReportCarOut :: getEnterVipType,"本地VIP").ne(ReportCarOut :: getInOperatorName,"System").ne(ReportCarOut :: getInOperatorName,"system").list();
//        // 遍历查重之前的
//        for (ReportCarOut reportCarOut : reportCarOutList) {
//            System.out.println("查重之前reportCarOut = " + reportCarOut);
//        }
//        ArrayList<ReportCarOut> result =
//                reportCarOutList.stream().collect(Collectors.collectingAndThen(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(ReportCarOut::getParkingCode))), ArrayList::new));
//        for (ReportCarOut reportCarOut : result) {
//            System.out.println("查重之后reportCarOut = " + reportCarOut);
//        }
        //        // 将reportCarOutList中的数组进行去除重复数据
//        reportCarOutList = reportCarOutList.stream().distinct().collect(Collectors.toList());
//        System.out.println("reportCarOutList = " + reportCarOutList);
//        //TODO  查询出来符合条件的reportCarOut的值，将这些值遍历取出每个值，将这些值的车牌号码在预约表中查询，若只有一条直接存储到reportCarOutReservations中，若有两条以上的话，判断车辆的进场时间是否为空，若不为空，匹配两条存储
//        for (int i = 0; i < reportCarOutList.size(); i++) {
//            List<VehicleReservation> list = vehicleReservationService.lambdaQuery().eq(VehicleReservation::getPlateNumber, reportCarOutList.get(i).getCarLicenseNumber()).eq(VehicleReservation :: getReserveFlag,1).list();
//            if (list.size() == 0) {
//                //查询出来的结果中没有包含
//                reportCarOutReservation.setEnterCarLicenseNumber(reportCarOutList.get(i).getEnterCarLicenseNumber());
//                reportCarOutReservation.setCreateTime(new Date());
//                reportCarOutReservation.setMerchantName(null);
//                reportCarOutReservation.setUpdateTime(new Date());
//                reportCarOutReservation.setReleaseReason(null);
//                reportCarOutReservation.setEnterChannelName(reportCarOutList.get(i).getEnterChannelName());
//                reportCarOutReservation.setEnterTime(reportCarOutList.get(i).getEnterTime());
//                reportCarOutReservation.setReleaseReason(null);
//                reportCarOutReservation.setLeaveTime(reportCarOutList.get(i).getLeaveTime());
//                reportCarOutReservation.setYardName(reportCarOutList.get(i).getYardName());
//                reportCarOutReservations.add(reportCarOutReservation);
//            }else if (list.size() == 1){
//                reportCarOutReservation.setEnterCarLicenseNumber(list.get(0).getPlateNumber());
//                reportCarOutReservation.setCreateTime(new Date());
//                reportCarOutReservation.setMerchantName(list.get(0).getMerchantName());
//                reportCarOutReservation.setCreateTime(new Date());
//                reportCarOutReservation.setReleaseReason(list.get(0).getReleaseReason());
//                reportCarOutReservation.setEnterChannelName(reportCarOutList.get(i).getEnterChannelName());
//                reportCarOutReservation.setEnterTime(reportCarOutList.get(i).getEnterTime());
//                reportCarOutReservation.setReleaseReason(list.get(0).getReleaseReason());
//                reportCarOutReservation.setLeaveTime(reportCarOutList.get(i).getLeaveTime());
//                reportCarOutReservation.setYardName(reportCarOutList.get(i).getYardName());
//                reportCarOutReservations.add(reportCarOutReservation);
//            }else if (list.size() > 1) {
//                // 出现两条以上的数据查询数据，遍历这些数据
//                for (int j = 0; j < list.size(); j++) {
//                    // 查询各个是否存在放行时间，只查询存在的数据，接着判断放行时间是否符合传入的开始时间和结束时间
//                    if (list.get(j).getReserveTime() != null) {
//                        // 将传入的String格式的startDate和endDate转为时间格式，和list中的每个放行时间
//                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//                        LocalDateTime enterTime = LocalDateTime.parse(startDate, formatter);
//                        LocalDateTime leaveTime = LocalDateTime.parse(endDate, formatter);
//                        LocalDateTime reserveTime = LocalDateTime.parse(list.get(j).getReserveTime(), formatter);
//                        if (reserveTime.isAfter(enterTime) && reserveTime.isBefore(leaveTime)) {
//                            reportCarOutReservation.setEnterCarLicenseNumber(list.get(j).getPlateNumber());
//                            reportCarOutReservation.setCreateTime(new Date());
//                            reportCarOutReservation.setMerchantName(list.get(j).getMerchantName());
//                            reportCarOutReservation.setCreateTime(new Date());
//                            reportCarOutReservation.setReleaseReason(list.get(j).getReleaseReason());
//                            reportCarOutReservation.setEnterChannelName(reportCarOutList.get(i).getEnterChannelName());
//                            reportCarOutReservation.setEnterTime(reportCarOutList.get(i).getEnterTime());
//                            reportCarOutReservation.setReleaseReason(list.get(j).getReleaseReason());
//                            reportCarOutReservation.setLeaveTime(reportCarOutList.get(i).getLeaveTime());
//                            reportCarOutReservation.setYardName(reportCarOutList.get(i).getYardName());
//                            reportCarOutReservations.add(reportCarOutReservation);
//                        }else {
//                            System.out.println("当前放行时间不合法" + list.get(i));
//                        }
//                        // 放行时间小于当前时间，并且预约表中车辆的进场时间为空，将该数据存储到reportCarOutReservations中
//                    }else {
//                        System.out.println("当前放行时间为空" + list.get(i));
//                    }
//                }
//            }
//        }
        return baseMapper.queryListReportOutExport(startDate,endDate,yardName);
    }

    @Override
    public List<ReportCarOutReservation> queryListReportCarOutExportLinShi(String startDate, String endDate, String yardName) {
        return baseMapper.queryListReportCarOutExportLinShi(startDate,endDate,yardName);
    }

    @Override
    public List<ReportCarOut> selectLeaveTime(String enterCarLicenseNumber,String enterTime, String startDate, String endDate) {
        return baseMapper.selectLeaveTime(enterCarLicenseNumber,enterTime,startDate,endDate);
    }

    @Override
    public List<ReportCarOut> selectCarRecords(String carCode,String enterTime) {
       return baseMapper.selectCarRecords(carCode,enterTime);
    }
}
