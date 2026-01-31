-- =============================================
-- 修复重复的 /vems/ 路径问题
-- 删除多余的 vems 层级
-- 创建时间：2025-12-17
-- =============================================

-- 使用数据库
USE `parking_management`;

-- =============================================
-- 1. 修复进场记录表中的重复 vems 路径
-- =============================================

-- 更新进场记录表 - 删除重复的 /vems/
UPDATE `report_car_in` 
SET `enter_car_full_picture` = REPLACE(`enter_car_full_picture`, '/vems/vems/', '/vems/')
WHERE `enter_car_full_picture` LIKE '%/vems/vems/%';

-- =============================================
-- 2. 修复离场记录表中的重复 vems 路径
-- =============================================

-- 更新离场记录表 - 进场照片字段
UPDATE `report_car_out` 
SET `enter_car_full_picture` = REPLACE(`enter_car_full_picture`, '/vems/vems/', '/vems/')
WHERE `enter_car_full_picture` LIKE '%/vems/vems/%';

-- 更新离场记录表 - 离场照片字段
UPDATE `report_car_out` 
SET `leave_car_full_picture` = REPLACE(`leave_car_full_picture`, '/vems/vems/', '/vems/')
WHERE `leave_car_full_picture` LIKE '%/vems/vems/%';

-- =============================================
-- 3. 验证修复结果
-- =============================================

-- 检查是否还有重复的 vems 路径
SELECT 
    '进场记录表 - 剩余重复路径' as table_info,
    COUNT(*) as count
FROM `report_car_in` 
WHERE `enter_car_full_picture` LIKE '%/vems/vems/%';

SELECT 
    '离场记录表 - 进场照片剩余重复路径' as table_info,
    COUNT(*) as count
FROM `report_car_out` 
WHERE `enter_car_full_picture` LIKE '%/vems/vems/%';

SELECT 
    '离场记录表 - 离场照片剩余重复路径' as table_info,
    COUNT(*) as count
FROM `report_car_out` 
WHERE `leave_car_full_picture` LIKE '%/vems/vems/%';

-- 显示修复后的数据示例
SELECT 
    id, 
    car_license_number, 
    enter_car_full_picture
FROM `report_car_in` 
WHERE enter_car_full_picture IS NOT NULL 
  AND enter_car_full_picture LIKE '%/vems/%'
LIMIT 5;

SELECT 
    id, 
    car_license_number, 
    enter_car_full_picture,
    leave_car_full_picture
FROM `report_car_out` 
WHERE (enter_car_full_picture IS NOT NULL AND enter_car_full_picture LIKE '%/vems/%')
   OR (leave_car_full_picture IS NOT NULL AND leave_car_full_picture LIKE '%/vems/%')
LIMIT 5;

-- =============================================
-- 脚本执行完成
-- =============================================
