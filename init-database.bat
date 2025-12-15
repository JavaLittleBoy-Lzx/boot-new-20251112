@echo off
chcp 65001 >nul
echo ========================================
echo    数据库初始化脚本
echo ========================================
echo.

echo 📋 准备执行 sys_user.sql 脚本...
echo.

REM 设置MySQL连接信息（从application.yml读取）
set MYSQL_HOST=localhost
set MYSQL_PORT=3306
set MYSQL_USER=root
set MYSQL_PASSWORD=123456
set MYSQL_DATABASE=parking_management

echo 🔧 连接信息：
echo    主机: %MYSQL_HOST%:%MYSQL_PORT%
echo    用户: %MYSQL_USER%
echo    数据库: %MYSQL_DATABASE%
echo.

REM 检查MySQL是否可用
mysql --version >nul 2>&1
if errorlevel 1 (
    echo ❌ 错误: 未找到MySQL命令
    echo    请确保MySQL已安装并添加到环境变量PATH中
    echo.
    echo 💡 手动执行方法：
    echo    1. 打开MySQL客户端或Navicat
    echo    2. 连接到数据库 %MYSQL_DATABASE%
    echo    3. 执行 sql\sys_user.sql 脚本
    echo.
    pause
    exit /b 1
)

echo ✅ MySQL已找到
echo.

REM 执行SQL脚本
echo 📝 执行 sys_user.sql...
mysql -h%MYSQL_HOST% -P%MYSQL_PORT% -u%MYSQL_USER% -p%MYSQL_PASSWORD% %MYSQL_DATABASE% < sql\sys_user.sql

if errorlevel 1 (
    echo.
    echo ❌ 数据库脚本执行失败！
    echo    可能的原因：
    echo    1. 数据库连接信息不正确
    echo    2. 数据库 %MYSQL_DATABASE% 不存在
    echo    3. 用户权限不足
    echo.
    echo 💡 解决方法：
    echo    1. 检查MySQL服务是否启动
    echo    2. 确认数据库名称、用户名、密码是否正确
    echo    3. 先创建数据库: 
    echo       CREATE DATABASE parking_management DEFAULT CHARSET=utf8mb4;
    echo.
    echo 📝 或者手动执行：
    echo    1. 打开MySQL客户端或Navicat
    echo    2. 连接到数据库
    echo    3. 执行 sql\sys_user.sql 脚本
    echo.
) else (
    echo.
    echo ========================================
    echo ✅ 数据库初始化成功！
    echo ========================================
    echo.
    echo 📋 已创建的测试账号：
    echo    ┌────────┬──────────┬──────────┐
    echo    │ 用户名 │   密码   │   角色   │
    echo    ├────────┼──────────┼──────────┤
    echo    │ admin  │  123456  │  管理员  │
    echo    │ user   │  123456  │ 普通用户 │
    echo    │ guest  │  123456  │  访客    │
    echo    └────────┴──────────┴──────────┘
    echo.
    echo 🎉 现在可以使用上述账号登录系统了！
    echo.
)

pause
