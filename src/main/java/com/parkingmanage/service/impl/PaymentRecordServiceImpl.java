package com.parkingmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.parkingmanage.entity.PaymentRecord;
import com.parkingmanage.mapper.PaymentRecordMapper;
import com.parkingmanage.service.PaymentRecordService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 缴费记录 服务实现类
 * </p>
 *
 * @author system
 * @since 2025-01-27
 */
@Service
public class PaymentRecordServiceImpl extends ServiceImpl<PaymentRecordMapper, PaymentRecord> implements PaymentRecordService {

    @Override
    public boolean existsByCarLicenseNumberAndPayTimeAndActualReceivable(String carLicenseNumber, String payTime, String actualReceivable) {
        QueryWrapper<PaymentRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("pay_time", payTime)
                   .eq("actual_receivable", actualReceivable)
                   .eq("deleted", 0);
        return count(queryWrapper) > 0;
    }
}
