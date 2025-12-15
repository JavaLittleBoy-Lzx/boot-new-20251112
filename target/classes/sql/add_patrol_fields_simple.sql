-- 为 patrol 表添加缺失的字段（简化版）
-- 用于修复 PatrolController 中 setCreatedate 和 setStatus 方法找不到的问题

-- 如果字段不存在，手动执行以下语句：

-- 添加创建日期字段
ALTER TABLE patrol ADD COLUMN createdate DATETIME COMMENT '创建日期';

-- 添加状态字段
ALTER TABLE patrol ADD COLUMN status VARCHAR(50) DEFAULT '待确认' COMMENT '状态';

-- 添加创建人字段
ALTER TABLE patrol ADD COLUMN createman VARCHAR(100) COMMENT '创建人';

-- 查看表结构
-- DESC patrol; 