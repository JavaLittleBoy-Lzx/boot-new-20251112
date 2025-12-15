-- 强制更新 visitor_application 表结构
-- 确保 owner_name 和 owner_phone 字段存在

-- 1. 首先检查并删除可能存在的旧字段（如果有的话）
SET @sql = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE table_name = 'visitor_application' 
    AND table_schema = DATABASE()
    AND column_name = 'owner_name') > 0,
    'ALTER TABLE visitor_application DROP COLUMN owner_name',
    'SELECT "owner_name字段不存在，跳过删除" as result');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE table_name = 'visitor_application' 
    AND table_schema = DATABASE()
    AND column_name = 'owner_phone') > 0,
    'ALTER TABLE visitor_application DROP COLUMN owner_phone',
    'SELECT "owner_phone字段不存在，跳过删除" as result');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 重新添加字段
ALTER TABLE visitor_application 
ADD COLUMN owner_name varchar(50) DEFAULT NULL COMMENT '业主姓名' AFTER phone;

ALTER TABLE visitor_application 
ADD COLUMN owner_phone varchar(20) DEFAULT NULL COMMENT '业主手机号' AFTER owner_name;

-- 3. 添加索引
ALTER TABLE visitor_application 
ADD INDEX idx_owner_phone (owner_phone);

-- 4. 更新测试数据（基于用户提供的截图数据）
UPDATE visitor_application 
SET owner_name = '刘英琦3-1-102', owner_phone = '13633620485'
WHERE application_no = 'VA20250628085928350' AND phone = '13593527970';

-- 5. 验证更新结果
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

-- 6. 显示表结构确认
DESCRIBE visitor_application;

-- 7. 显示所有包含业主信息的记录
SELECT 
    id,
    application_no,
    nickname,
    phone,
    owner_name,
    owner_phone,
    auditstatus
FROM visitor_application 
WHERE owner_name IS NOT NULL AND owner_name != ''
ORDER BY applydate DESC
LIMIT 10; 