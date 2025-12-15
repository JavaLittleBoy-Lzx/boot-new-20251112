-- 检查 visitor_application 表结构和数据
-- 用于诊断业主信息字段问题

-- 1. 检查表结构
SHOW CREATE TABLE visitor_application;

-- 2. 检查所有字段
DESCRIBE visitor_application;

-- 3. 检查是否存在 owner_name 和 owner_phone 字段
SELECT 
    COLUMN_NAME as '字段名',
    DATA_TYPE as '数据类型',
    IS_NULLABLE as '允许空值',
    COLUMN_DEFAULT as '默认值',
    COLUMN_COMMENT as '注释'
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'visitor_application'
  AND COLUMN_NAME IN ('owner_name', 'owner_phone')
ORDER BY ORDINAL_POSITION;

-- 4. 查看具体的数据记录（特别是 application_no = 'VA20250628085928350'）
SELECT 
    id,
    application_no,
    nickname,
    phone,
    CASE 
        WHEN EXISTS (
            SELECT 1 FROM information_schema.COLUMNS 
            WHERE TABLE_SCHEMA = DATABASE() 
              AND TABLE_NAME = 'visitor_application' 
              AND COLUMN_NAME = 'owner_name'
        ) THEN '字段存在' 
        ELSE '字段不存在' 
    END as owner_name_status,
    CASE 
        WHEN EXISTS (
            SELECT 1 FROM information_schema.COLUMNS 
            WHERE TABLE_SCHEMA = DATABASE() 
              AND TABLE_NAME = 'visitor_application' 
              AND COLUMN_NAME = 'owner_phone'
        ) THEN '字段存在' 
        ELSE '字段不存在' 
    END as owner_phone_status
FROM visitor_application 
WHERE application_no = 'VA20250628085928350';

-- 5. 如果字段存在，查看实际数据
-- 注意：如果字段不存在，这个查询会报错，这是正常的
SELECT 
    id,
    application_no,
    nickname,
    phone,
    owner_name,
    owner_phone,
    auditstatus,
    applydate
FROM visitor_application 
WHERE application_no = 'VA20250628085928350';

-- 6. 检查所有包含业主信息的记录
SELECT 
    COUNT(*) as total_records,
    COUNT(CASE WHEN owner_name IS NOT NULL AND owner_name != '' THEN 1 END) as records_with_owner_name,
    COUNT(CASE WHEN owner_phone IS NOT NULL AND owner_phone != '' THEN 1 END) as records_with_owner_phone
FROM visitor_application; 