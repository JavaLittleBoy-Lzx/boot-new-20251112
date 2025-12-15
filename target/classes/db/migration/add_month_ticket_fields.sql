-- 添加月票关联字段以支持月票车违章记录
-- 执行时间：2025-01-31

-- 1. 为violations表添加month_ticket_id字段
ALTER TABLE violations 
ADD COLUMN month_ticket_id INT COMMENT '月票ID，关联month_tick表' AFTER owner_id;

-- 2. 为violations表添加is_monthly_ticket字段
ALTER TABLE violations 
ADD COLUMN is_monthly_ticket TINYINT(1) DEFAULT 0 COMMENT '是否月票车' AFTER month_ticket_id;

-- 3. 添加外键约束（可选，如果month_tick表存在）
-- ALTER TABLE violations 
-- ADD CONSTRAINT fk_violation_month_ticket 
-- FOREIGN KEY (month_ticket_id) REFERENCES month_tick(id);

-- 4. 添加索引以提高查询性能
CREATE INDEX idx_violations_month_ticket_id ON violations(month_ticket_id);
CREATE INDEX idx_violations_is_monthly_ticket ON violations(is_monthly_ticket);

-- 5. 为月票车创建查询视图（可选）
CREATE OR REPLACE VIEW v_monthly_ticket_violations AS
SELECT 
    v.*,
    mt.ticket_name,
    mt.owner_name as month_ticket_owner_name,
    mt.owner_phone as month_ticket_owner_phone,
    mt.parking_spot
FROM violations v
JOIN month_tick mt ON v.month_ticket_id = mt.id
WHERE v.is_monthly_ticket = 1;

-- 6. 添加注释
ALTER TABLE violations MODIFY COLUMN month_ticket_id INT COMMENT '月票ID，关联month_tick表，用于月票车违章记录';
ALTER TABLE violations MODIFY COLUMN is_monthly_ticket TINYINT(1) DEFAULT 0 COMMENT '是否月票车，1=是，0=否';

-- 查看表结构（用于验证）
-- DESCRIBE violations; 