package com.parkingmanage.handle;


import cn.hutool.core.util.StrUtil;
import com.parkingmanage.utils.AesUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @Description:
 * @PROJECT_NAME: parkingmanage
 * @PACKAGE_NAME: com.parkingmanage.handle
 * @NAME: EncryptHandler
 * @author:yuli
 * @Version: 1.0
 * @DATE: 2021/11/2 15:28
 */
/** 数据库中的数据类型 */
@MappedJdbcTypes(JdbcType.VARCHAR)
//** 处理后的数据类型 */
@MappedTypes(value = String.class)
@Slf4j
public class EncryptHandler extends BaseTypeHandler<String> {

    @Override
    public void setNonNullParameter(PreparedStatement preparedStatement, int i, String s, JdbcType jdbcType) throws SQLException {
        try {
            if (StrUtil.isBlank(s)) {
                return;
            }
            String encrypt = AesUtil.encrypt(s);
            preparedStatement.setString(i, encrypt);
        } catch (Exception e) {

            log.error("typeHandler加密异常：" + e);
        }
    }

    @Override
    public String getNullableResult(ResultSet resultSet, String s) throws SQLException {
        String col = resultSet.getString(s);
        try {
            if (StrUtil.isBlank(col)) {
                return col;
            }
            return AesUtil.decrypt(col);
        } catch (Exception e) {
            log.error("typeHandler解密异常：" + e);
        }
        return col;
    }

    @Override
    public String getNullableResult(ResultSet resultSet, int i) throws SQLException {
        String col = resultSet.getString(i);
        try {
            if (StrUtil.isBlank(col)) {
                return col;
            }
            return AesUtil.decrypt(col);
        } catch (Exception e) {
            log.error("typeHandler解密异常：" + e);
        }
        return col;
    }

    @Override
    public String getNullableResult(CallableStatement callableStatement, int i) throws SQLException {
        String col = callableStatement.getString(i);
        try {
            if (StrUtil.isBlank(col)) {
                return col;
            }
            return AesUtil.decrypt(col);
        } catch (Exception e) {
            log.error("typeHandler解密异常：" + e);
        }
        return col;
    }
}
