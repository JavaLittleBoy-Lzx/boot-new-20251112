-- 访客申请表添加业主信息字段
-- 执行时间：2025-06-27

-- 1. 检查字段是否已存在，如果不存在则添加
SET @sql = '';
SELECT COUNT(*) INTO @count 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'visitor_application' 
  AND COLUMN_NAME = 'owner_name';

IF @count = 0 THEN
    SET @sql = 'ALTER TABLE visitor_application ADD COLUMN owner_name varchar(50) DEFAULT NULL COMMENT ''业主姓名'' AFTER phone';
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
    SELECT 'Added owner_name column' as result;
ELSE
    SELECT 'owner_name column already exists' as result;
END IF;

-- 2. 检查业主手机号字段是否已存在，如果不存在则添加
SELECT COUNT(*) INTO @count 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'visitor_application' 
  AND COLUMN_NAME = 'owner_phone';

IF @count = 0 THEN
    SET @sql = 'ALTER TABLE visitor_application ADD COLUMN owner_phone varchar(20) DEFAULT NULL COMMENT ''业主手机号'' AFTER owner_name';
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
    SELECT 'Added owner_phone column' as result;
ELSE
    SELECT 'owner_phone column already exists' as result;
END IF;

-- 3. 添加索引（如果不存在）
SELECT COUNT(*) INTO @count 
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'visitor_application' 
  AND INDEX_NAME = 'idx_owner_phone';

IF @count = 0 THEN
    SET @sql = 'ALTER TABLE visitor_application ADD INDEX idx_owner_phone (owner_phone)';
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
    SELECT 'Added idx_owner_phone index' as result;
ELSE
    SELECT 'idx_owner_phone index already exists' as result;
END IF;

-- 4. 显示表结构确认
DESCRIBE visitor_application;

-- 5. 显示更新后的完整表信息
SELECT 
    COLUMN_NAME as '字段名',
    DATA_TYPE as '数据类型',
    IS_NULLABLE as '允许空值',
    COLUMN_DEFAULT as '默认值',
    COLUMN_COMMENT as '注释'
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'visitor_application'
ORDER BY ORDINAL_POSITION; 