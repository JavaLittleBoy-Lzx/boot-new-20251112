-- 插入测试管家数据
-- 确保数据库中有对应的管家记录用于测试

-- 删除已存在的测试数据（如果有）
DELETE FROM butler WHERE phone = '13593527970';

-- 插入测试管家数据
INSERT INTO butler (
    usercode, 
    username, 
    phone, 
    province, 
    city, 
    district, 
    community, 
    createdate, 
    status, 
    openid
) VALUES (
    'BUTLER001',
    '张管家',
    '13593527970',
    '黑龙江省',
    '哈尔滨市',
    '道里区',
    '欧洲新城',
    NOW(),
    '已确认',
    'manager_13593527970_test'
);

-- 验证插入结果
SELECT * FROM butler WHERE phone = '13593527970'; 