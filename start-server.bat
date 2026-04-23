@echo off
echo ========================================
echo EV电池交易平台后端服务启动脚本
echo ========================================

cd /d "d:\HuaweiMoveData\Users\hbj\Desktop\EV-Battery\ev-battery-platform-backend"

echo 正在启动后端服务...
echo 服务将在 http://localhost:8081 启动
echo 
echo 测试账号信息：
echo 管理员账号: admin / 123456
echo 测试用户: testuser / 123456
echo 采购用户: buyer01 / 123456
echo 销售用户: seller01 / 123456
echo 

"d:\HuaweiMoveData\Users\hbj\Desktop\EV-Battery\apache-maven-3.9.9\bin\mvn.cmd" -Dspring-boot.run.profiles=dev spring-boot:run