-- 添加创建者字段以支持用户权限控制
-- 执行时间：2025-01-31

-- 1. 为violations表添加created_by字段
ALTER TABLE violations 
ADD COLUMN created_by VARCHAR(50) COMMENT '创建者ID';

-- 2. 为existing records设置默认创建者（可选）
UPDATE violations 
SET created_by = 'system' 
WHERE created_by IS NULL;

-- 3. 添加索引以提高查询性能
CREATE INDEX idx_violations_created_by ON violations(created_by);

-- 4. 创建用于权限控制的视图（可选）
CREATE OR REPLACE VIEW v_user_violations AS
SELECT 
    v.*,
    o.ownername as owner_name,
    o.ownerphone as owner_phone,
    o.building,
    o.units,
    o.roomnumber,
    o.creditScore as credit_score
FROM violations v
LEFT JOIN ownerinfo o ON v.owner_id = o.id;

-- 添加注释
ALTER TABLE violations MODIFY COLUMN created_by VARCHAR(50) COMMENT '创建者ID，用于数据权限控制';

-- 查看表结构（用于验证）
-- DESCRIBE violations; 