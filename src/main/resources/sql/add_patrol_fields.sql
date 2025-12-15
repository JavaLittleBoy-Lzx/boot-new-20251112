-- 为 patrol 表添加缺失的字段
-- 用于修复 PatrolController 中 setCreatedate 和 setStatus 方法找不到的问题

-- 检查并添加 createdate 字段
SET @column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'patrol'
    AND COLUMN_NAME = 'createdate'
);

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE patrol ADD COLUMN createdate DATETIME COMMENT "创建日期"',
    'SELECT "createdate 字段已存在" AS result'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 status 字段
SET @column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'patrol'
    AND COLUMN_NAME = 'status'
);

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE patrol ADD COLUMN status VARCHAR(50) DEFAULT "待确认" COMMENT "状态"',
    'SELECT "status 字段已存在" AS result'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 createman 字段
SET @column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'patrol'
    AND COLUMN_NAME = 'createman'
);

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE patrol ADD COLUMN createman VARCHAR(100) COMMENT "创建人"',
    'SELECT "createman 字段已存在" AS result'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 显示当前 patrol 表结构
DESC patrol; 