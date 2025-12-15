-- 检查管家表中地址数据的完整性
-- 查看所有管家的地址信息
SELECT 
    id,
    usercode,
    username,
    phone,
    province,
    city,
    district,
    community,
    status,
    createdate
FROM butler 
ORDER BY id;

-- 统计地址字段的空值情况
SELECT 
    '总记录数' as field_name,
    COUNT(*) as total_count,
    0 as null_count,
    0 as empty_count
FROM butler
UNION ALL
SELECT 
    'province字段',
    COUNT(*) as total_count,
    SUM(CASE WHEN province IS NULL THEN 1 ELSE 0 END) as null_count,
    SUM(CASE WHEN province = '' OR TRIM(province) = '' THEN 1 ELSE 0 END) as empty_count
FROM butler
UNION ALL
SELECT 
    'city字段',
    COUNT(*) as total_count,
    SUM(CASE WHEN city IS NULL THEN 1 ELSE 0 END) as null_count,
    SUM(CASE WHEN city = '' OR TRIM(city) = '' THEN 1 ELSE 0 END) as empty_count
FROM butler
UNION ALL
SELECT 
    'district字段',
    COUNT(*) as total_count,
    SUM(CASE WHEN district IS NULL THEN 1 ELSE 0 END) as null_count,
    SUM(CASE WHEN district = '' OR TRIM(district) = '' THEN 1 ELSE 0 END) as empty_count
FROM butler
UNION ALL
SELECT 
    'community字段',
    COUNT(*) as total_count,
    SUM(CASE WHEN community IS NULL THEN 1 ELSE 0 END) as null_count,
    SUM(CASE WHEN community = '' OR TRIM(community) = '' THEN 1 ELSE 0 END) as empty_count
FROM butler;

-- 查找地址信息不完整的管家记录
SELECT 
    id,
    username,
    phone,
    province,
    city,
    district,
    community,
    '缺少省份' as issue
FROM butler 
WHERE province IS NULL OR province = '' OR TRIM(province) = ''
UNION ALL
SELECT 
    id,
    username,
    phone,
    province,
    city,
    district,
    community,
    '缺少城市' as issue
FROM butler 
WHERE city IS NULL OR city = '' OR TRIM(city) = ''
UNION ALL
SELECT 
    id,
    username,
    phone,
    province,
    city,
    district,
    community,
    '缺少区县' as issue
FROM butler 
WHERE district IS NULL OR district = '' OR TRIM(district) = ''
UNION ALL
SELECT 
    id,
    username,
    phone,
    province,
    city,
    district,
    community,
    '缺少小区' as issue
FROM butler 
WHERE community IS NULL OR community = '' OR TRIM(community) = ''
ORDER BY id;

-- 根据图片中的数据更新示例（需要根据实际情况修改）
-- 假设需要为现有管家补充地址信息
UPDATE butler 
SET 
    province = '黑龙江省',
    city = '哈尔滨市',
    district = '香坊区'
WHERE usercode = '001' AND username = '测试001';

UPDATE butler 
SET 
    province = '黑龙江省',
    city = '哈尔滨市',
    district = '道里区'
WHERE usercode = '003' AND username = '小李';

-- 验证更新结果
SELECT 
    id,
    usercode,
    username,
    phone,
    province,
    city,
    district,
    community,
    '地址完整' as status
FROM butler 
WHERE province IS NOT NULL AND province != '' AND TRIM(province) != ''
  AND city IS NOT NULL AND city != '' AND TRIM(city) != ''
  AND district IS NOT NULL AND district != '' AND TRIM(district) != ''
  AND community IS NOT NULL AND community != '' AND TRIM(community) != ''
ORDER BY id; 