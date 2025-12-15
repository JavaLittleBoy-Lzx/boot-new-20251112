-- =====================================================
-- 为acms_event_record表添加未预约纯访客标识
-- 功能：在acms_event_record表中添加字段，用于标识未预约的纯访客
-- 说明：此功能仅针对刷身份证进出（event_type = 197162）的记录
--       通过查询人员姓名，检查事件时间是否在预约时间段内来判断
-- 创建时间：2025-01-XX
-- =====================================================

-- 1. 添加未预约纯访客标识字段
-- 说明：如果字段已存在，此脚本会报错，可以忽略错误或先删除字段再执行

-- 方式1：直接添加（推荐，如果确定字段不存在）
ALTER TABLE `acms_event_record` 
ADD COLUMN `is_unreserved_visitor` TINYINT(1) DEFAULT 0 
    COMMENT '是否未预约纯访客：0-否（已预约或有预约记录），1-是（未预约的纯访客）。仅针对刷身份证进出（event_type=197162）的记录' 
    AFTER `vip_type_name`,
ALTER TABLE `acms_event_record` ADD COLUMN `reservation_time_range` VARCHAR(100) DEFAULT NULL COMMENT '预约时间段（格式：开始时间-结束时间），例如：2025-01-15 10:00:00-2025-01-15 12:00:00。仅针对已预约的访客' 
    AFTER `is_unreserved_visitor`;

-- 方式2：先检查再添加（兼容性更好，如果字段已存在则跳过）
-- SET @dbname = DATABASE();
-- SET @tablename = 'acms_event_record';
-- SET @columnname = 'is_unreserved_visitor';
-- SET @preparedStatement = (SELECT IF(
--   (
--     SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
--     WHERE
--       (table_name = @tablename)
--       AND (table_schema = @dbname)
--       AND (column_name = @columnname)
--   ) > 0,
--   'SELECT 1',
--   CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' TINYINT(1) DEFAULT 0 COMMENT ''是否未预约纯访客：0-否（已预约或有预约记录），1-是（未预约的纯访客）。仅针对刷身份证进出（event_type=197162）的记录'' AFTER vip_type_name')
-- ));
-- PREPARE alterIfNotExists FROM @preparedStatement;
-- EXECUTE alterIfNotExists;
-- DEALLOCATE PREPARE alterIfNotExists;

-- =====================================================
-- 2. 更新逻辑：根据姓名和时间匹配预约记录
-- =====================================================

-- 说明：对于刷身份证进出（event_type = 197162）的记录
--       1. 通过person_name查询visitor_reservation_sync表
--       2. 检查event_time是否在预约时间段内（优先使用gatewayTransitBeginTime/gatewayTransitEndTime，否则使用startTime/endTime）
--       3. 如果找到匹配的预约记录，更新vip_type_name为预约记录的vipTypeName，is_unreserved_visitor设为0
--       4. 如果未找到匹配的预约记录，is_unreserved_visitor设为1，vip_type_name保持NULL或清空

-- 方式1：一次性更新所有符合条件的记录
-- 说明：对于刷身份证进出的记录，通过姓名和时间匹配预约记录
--       如果找到匹配的预约记录（事件时间在预约时间段内），则更新vip_type_name并标记为已预约
--       如果未找到匹配的预约记录，则标记为未预约的纯访客

UPDATE `acms_event_record` aer
LEFT JOIN (
    SELECT 
        vrs.visitor_name,
        vrs.vip_type_name,
        -- 优先使用网关通行时间，如果为空则使用预约时间
        COALESCE(vrs.gateway_transit_begin_time, vrs.start_time) AS effective_start_time,
        COALESCE(vrs.gateway_transit_end_time, vrs.end_time) AS effective_end_time,
        -- 构建预约时间段字符串（格式：开始时间-结束时间）
        CONCAT(
            DATE_FORMAT(COALESCE(vrs.gateway_transit_begin_time, vrs.start_time), '%Y-%m-%d %H:%i:%s'),
            '-',
            DATE_FORMAT(COALESCE(vrs.gateway_transit_end_time, vrs.end_time), '%Y-%m-%d %H:%i:%s')
        ) AS time_range_str
    FROM `visitor_reservation_sync` vrs
    WHERE vrs.deleted = 0
        AND vrs.visitor_name IS NOT NULL
        AND vrs.visitor_name != ''
        AND (
            (vrs.gateway_transit_begin_time IS NOT NULL AND vrs.gateway_transit_end_time IS NOT NULL)
            OR (vrs.start_time IS NOT NULL AND vrs.end_time IS NOT NULL)
        )
) vrs ON aer.person_name = vrs.visitor_name
    AND aer.event_time >= vrs.effective_start_time
    AND aer.event_time <= vrs.effective_end_time
SET 
    aer.vip_type_name = COALESCE(vrs.vip_type_name, aer.vip_type_name),
    aer.is_unreserved_visitor = IF(vrs.visitor_name IS NULL, 1, 0),
    aer.reservation_time_range = IF(vrs.visitor_name IS NULL, NULL, vrs.time_range_str)
WHERE aer.event_type = 197162  -- 仅处理刷身份证进出的记录（人证比对）
    AND aer.deleted = 0
    AND aer.person_name IS NOT NULL
    AND aer.person_name != '';

-- =====================================================
-- 补充更新：确保所有未匹配的刷身份证进出记录都标记为未预约
-- =====================================================

-- 将 is_unreserved_visitor 为 NULL 或 0，但没有匹配预约记录的记录标记为未预约（纯访客）
UPDATE `acms_event_record` aer
LEFT JOIN (
    SELECT DISTINCT
        vrs.visitor_name,
        -- 优先使用网关通行时间，如果为空则使用预约时间
        COALESCE(vrs.gateway_transit_begin_time, vrs.start_time) AS effective_start_time,
        COALESCE(vrs.gateway_transit_end_time, vrs.end_time) AS effective_end_time
    FROM `visitor_reservation_sync` vrs
    WHERE vrs.deleted = 0
        AND vrs.visitor_name IS NOT NULL
        AND vrs.visitor_name != ''
        AND (
            (vrs.gateway_transit_begin_time IS NOT NULL AND vrs.gateway_transit_end_time IS NOT NULL)
            OR (vrs.start_time IS NOT NULL AND vrs.end_time IS NOT NULL)
        )
) vrs ON aer.person_name = vrs.visitor_name
    AND aer.event_time >= vrs.effective_start_time
    AND aer.event_time <= vrs.effective_end_time
SET 
    aer.is_unreserved_visitor = 1,
    aer.vip_type_name = NULL,
    aer.reservation_time_range = NULL
WHERE aer.event_type = 197162  -- 仅处理刷身份证进出的记录
    AND aer.deleted = 0
    AND aer.person_name IS NOT NULL
    AND aer.person_name != ''
    AND aer.event_time IS NOT NULL
    AND vrs.visitor_name IS NULL  -- 未找到匹配的预约记录
    AND (aer.is_unreserved_visitor IS NULL OR aer.is_unreserved_visitor = 0);  -- 未标注或标注为已预约

-- =====================================================
-- 方式2：使用存储过程实现实时更新（可选）
-- =====================================================

-- DELIMITER $$
-- 
-- CREATE PROCEDURE IF NOT EXISTS `update_visitor_reservation_status`()
-- BEGIN
--     DECLARE done INT DEFAULT FALSE;
--     DECLARE v_id BIGINT;
--     DECLARE v_person_name VARCHAR(100);
--     DECLARE v_event_time DATETIME;
--     DECLARE v_vip_type_name VARCHAR(100);
--     DECLARE v_is_unreserved TINYINT(1);
--     
--     -- 游标：查询所有刷身份证进出的未处理记录
--     DECLARE cur CURSOR FOR 
--         SELECT id, person_name, event_time
--         FROM acms_event_record
--         WHERE event_type = 197162
--             AND deleted = 0
--             AND person_name IS NOT NULL
--             AND person_name != ''
--             AND (is_unreserved_visitor IS NULL OR is_unreserved_visitor = 0);
--     
--     DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
--     
--     OPEN cur;
--     
--     read_loop: LOOP
--         FETCH cur INTO v_id, v_person_name, v_event_time;
--         
--         IF done THEN
--             LEAVE read_loop;
--         END IF;
--         
--         -- 查询匹配的预约记录
--         SELECT vip_type_name INTO v_vip_type_name
--         FROM visitor_reservation_sync
--         WHERE visitor_name = v_person_name
--             AND deleted = 0
--             AND (
--                 -- 优先使用网关通行时间
--                 (gateway_transit_begin_time IS NOT NULL 
--                  AND gateway_transit_end_time IS NOT NULL
--                  AND v_event_time >= gateway_transit_begin_time
--                  AND v_event_time <= gateway_transit_end_time)
--                 OR
--                 -- 否则使用预约时间
--                 (start_time IS NOT NULL
--                  AND end_time IS NOT NULL
--                  AND v_event_time >= start_time
--                  AND v_event_time <= end_time)
--             )
--         LIMIT 1;
--         
--         -- 判断是否为未预约访客
--         IF v_vip_type_name IS NULL THEN
--             SET v_is_unreserved = 1;
--             SET v_vip_type_name = NULL;
--         ELSE
--             SET v_is_unreserved = 0;
--         END IF;
--         
--         -- 更新记录
--         UPDATE acms_event_record
--         SET vip_type_name = v_vip_type_name,
--             is_unreserved_visitor = v_is_unreserved
--         WHERE id = v_id;
--         
--     END LOOP;
--     
--     CLOSE cur;
-- END$$
-- 
-- DELIMITER ;

-- =====================================================
-- 3. 创建索引以提高查询性能（可选）
-- =====================================================

-- 为is_unreserved_visitor字段添加索引（如果需要根据此字段查询）
-- ALTER TABLE `acms_event_record` ADD INDEX `idx_is_unreserved_visitor` (`is_unreserved_visitor`);

-- 为visitor_reservation_sync表的visitor_name字段添加索引（如果还没有）
-- ALTER TABLE `visitor_reservation_sync` ADD INDEX IF NOT EXISTS `idx_visitor_name` (`visitor_name`);

-- =====================================================
-- 4. 验证字段是否添加成功
-- =====================================================

-- SELECT 
--     COLUMN_NAME,
--     DATA_TYPE,
--     CHARACTER_MAXIMUM_LENGTH,
--     COLUMN_COMMENT,
--     IS_NULLABLE,
--     COLUMN_DEFAULT
-- FROM INFORMATION_SCHEMA.COLUMNS
-- WHERE TABLE_SCHEMA = DATABASE()
--   AND TABLE_NAME = 'acms_event_record'
--   AND COLUMN_NAME = 'is_unreserved_visitor';

-- =====================================================
-- 5. 验证更新结果
-- =====================================================

-- 查询未预约的纯访客记录
-- SELECT 
--     id,
--     event_id,
--     person_name,
--     event_time,
--     direction,
--     channel_name,
--     vip_type_name,
--     is_unreserved_visitor
-- FROM acms_event_record
-- WHERE event_type = 197162
--     AND is_unreserved_visitor = 1
--     AND deleted = 0
-- ORDER BY event_time DESC
-- LIMIT 100;

-- 查询已预约的访客记录
-- SELECT 
--     aer.id,
--     aer.event_id,
--     aer.person_name,
--     aer.event_time,
--     aer.direction,
--     aer.vip_type_name,
--     aer.is_unreserved_visitor,
--     vrs.reservation_id,
--     vrs.start_time,
--     vrs.end_time
-- FROM acms_event_record aer
-- LEFT JOIN visitor_reservation_sync vrs ON aer.person_name = vrs.visitor_name
-- WHERE aer.event_type = 197162
--     AND aer.is_unreserved_visitor = 0
--     AND aer.deleted = 0
-- ORDER BY aer.event_time DESC
-- LIMIT 100;

-- =====================================================
-- 6. 回滚脚本（如果需要删除字段）
-- =====================================================

-- ALTER TABLE `acms_event_record` 
-- DROP COLUMN IF EXISTS `is_unreserved_visitor`;

