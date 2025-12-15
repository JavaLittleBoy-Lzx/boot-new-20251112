#!/bin/bash
# 访客预约查询接口测试脚本

# 配置
BASE_URL="http://www.xuerparking.cn:8080"
API_PATH="/parking/nefuData/getVisitorReservationByCarNumber"

echo "========================================"
echo "访客预约查询接口测试"
echo "========================================"
echo ""

# 测试1: 正常查询
echo "📋 测试1: 正常查询车牌号"
echo "----------------------------------------"
CAR_NUMBER="京A12345"
echo "请求: GET ${BASE_URL}${API_PATH}?carNumber=${CAR_NUMBER}"
curl -X GET "${BASE_URL}${API_PATH}?carNumber=${CAR_NUMBER}" \
  -H "Content-Type: application/json" \
  | python -m json.tool
echo ""
echo ""

# 测试2: 查询不存在的车牌号
echo "📋 测试2: 查询不存在的车牌号"
echo "----------------------------------------"
CAR_NUMBER="不存在的车牌"
echo "请求: GET ${BASE_URL}${API_PATH}?carNumber=${CAR_NUMBER}"
curl -X GET "${BASE_URL}${API_PATH}?carNumber=${CAR_NUMBER}" \
  -H "Content-Type: application/json" \
  | python -m json.tool
echo ""
echo ""

# 测试3: 车牌号为空
echo "📋 测试3: 车牌号为空（预期返回错误）"
echo "----------------------------------------"
echo "请求: GET ${BASE_URL}${API_PATH}?carNumber="
curl -X GET "${BASE_URL}${API_PATH}?carNumber=" \
  -H "Content-Type: application/json" \
  | python -m json.tool
echo ""
echo ""

# 测试4: 不传车牌号参数
echo "📋 测试4: 不传车牌号参数（预期返回错误）"
echo "----------------------------------------"
echo "请求: GET ${BASE_URL}${API_PATH}"
curl -X GET "${BASE_URL}${API_PATH}" \
  -H "Content-Type: application/json" \
  | python -m json.tool
echo ""
echo ""

echo "========================================"
echo "测试完成"
echo "========================================"
