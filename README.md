# EV-BatterySecondLife

EV-BatterySecondLife 是一个面向新能源汽车动力电池二次利用场景的全栈平台，覆盖电池档案管理、健康评估、梯次利用交易、合同存证、物流追踪、运营统计、消息通知和后台 AI 助手等功能。

## 技术栈

- 前端：Vue 3、Vite、Element Plus、Pinia、Vue Router、ECharts
- 后端：Spring Boot 2.7、MyBatis-Plus、Spring Security、JWT、Redis、MySQL
- 机器学习：Python 模型推理，用于电池 SOH 预测
- 文档能力：PDF 合同生成与验真

## 项目结构

```text
.
|-- dataset/                         # 示例电池数据、BMS 校验数据、合同测试文件
|-- ev-battery-platform-backend/      # Spring Boot 后端服务
|   |-- src/main/java/com/evbattery/
|   |   |-- modules/admin             # 后台管理与驾驶舱接口
|   |   |-- modules/assessment        # 电池评估与模型预测流程
|   |   |-- modules/battery           # 电池档案管理
|   |   |-- modules/contract          # 合同生成、存证与验真
|   |   |-- modules/logistics         # 物流追踪
|   |   |-- modules/message           # 消息通知
|   |   |-- modules/statistics        # 运营统计
|   |   |-- modules/trade             # 商品、购物车、订单交易流程
|   |   `-- modules/user              # 登录注册、用户资料与账号能力
|   `-- src/main/resources            # 后端配置文件
|-- ev-battery-platform-frontend/     # Vue 前端应用
|   |-- public/                       # 页面图片与静态资源
|   `-- src/
|       |-- api                       # 前端接口封装
|       |-- components                # 公共组件
|       |-- layout                    # 用户端与后台布局
|       |-- store                     # Pinia 状态管理
|       `-- views                     # 页面视图
|-- ml/                               # 电池健康预测模型与推理脚本
|-- tools/                            # 辅助工具脚本
`-- alter_tables.sql                  # 数据库表结构调整脚本
```

`node_modules/`、`dist/`、`target/`、运行日志、缓存目录、后端运行存储文件等均属于生成文件，已经通过 `.gitignore` 忽略，不应提交到仓库。

## 环境要求

- JDK 8 或以上
- Maven 3.8 或以上
- Node.js 18 或以上
- Python 3.8 或以上
- MySQL 8
- Redis

## 后端启动

1. 创建 MySQL 数据库，并导入项目所需表结构和基础数据。
2. 根据本地环境修改 `ev-battery-platform-backend/src/main/resources/application-dev.yml` 中的数据库、Redis 等配置。
3. 如需启用后台 AI 助手，请配置环境变量 `ZHIPU_API_KEY`。
4. 启动后端服务：

```bash
cd ev-battery-platform-backend
mvn spring-boot:run
```

后端默认通过 `../ml/predict.py` 调用机器学习预测脚本。

## 前端启动

```bash
cd ev-battery-platform-frontend
npm install
npm run dev
```

生产构建检查：

```bash
npm run build
```

## 机器学习模块

```bash
cd ml
pip install -r requirements.txt
python predict.py
```

仓库中包含示例模型文件和测试数据，可用于本地预测和功能联调。

## 主要功能

- 用户注册、登录、个人资料与后台账号管理
- 电池档案浏览、详情查看与健康评估
- 基于 Python 模型的 SOH 预测
- 梯次利用商品发布、商品详情、购物车、订单交易流程
- 合同创建、PDF 生成、合同验真
- 物流追踪与地图展示
- 后台驾驶舱、用户管理、订单管理、商品审核、电池审核、合同管理和运营统计
- 消息中心与后台 AI 助手


