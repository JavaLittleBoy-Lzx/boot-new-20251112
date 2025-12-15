-- 访客申请表添加业主信息字段（简化版本）
-- 执行时间：2025-06-27
-- 说明：为访客申请表添加业主姓名和业主手机号字段，用于在访客预约时自动填充业主信息

-- 添加业主姓名字段
ALTER TABLE visitor_application 
ADD COLUMN IF NOT EXISTS owner_name varchar(50) DEFAULT NULL COMMENT '业主姓名' AFTER phone;

-- 添加业主手机号字段  
ALTER TABLE visitor_application 
ADD COLUMN IF NOT EXISTS owner_phone varchar(20) DEFAULT NULL COMMENT '业主手机号' AFTER owner_name;

-- 添加业主手机号索引（提高查询性能）
ALTER TABLE visitor_application 
ADD INDEX IF NOT EXISTS idx_owner_phone (owner_phone);

-- 显示表结构确认
DESCRIBE visitor_application;

-- 查看新增字段
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