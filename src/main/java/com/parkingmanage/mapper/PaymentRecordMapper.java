package com.parkingmanage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parkingmanage.entity.PaymentRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 缴费记录 Mapper 接口
 * </p>
 *
 * @author system
 * @since 2025-01-27
 */
@Mapper
public interface PaymentRecordMapper extends BaseMapper<PaymentRecord> {

}
