package com.parkingmanage.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.parkingmanage.entity.PaymentRecord;

/**
 * <p>
 * 缴费记录 服务类
 * </p>
 *
 * @author system
 * @since 2025-01-27
 */
public interface PaymentRecordService extends IService<PaymentRecord> {

    /**
     * 检查缴费记录是否已存在
     * @param carLicenseNumber 车牌号码
     * @param payTime 支付时间
     * @param actualReceivable 实际应收金额
     * @return 是否存在
     */
    boolean existsByCarLicenseNumberAndPayTimeAndActualReceivable(String carLicenseNumber, String payTime, String actualReceivable);
}
