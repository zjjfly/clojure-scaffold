# Clojure Web Application Scaffold

中文 | [English](README_EN.md)

一个功能完整的 Clojure Web 应用脚手架，集成了现代 Web 开发所需的核心功能。

## 技术栈

### 核心框架
- **Clojure 1.12.4** - 编程语言
- **Ring** - HTTP 服务器抽象
- **Reitit** - 路由库
- **Integrant** - 系统组件管理

### 数据处理
- **Muuntaja** - 内容协商和格式转换
- **Malli** - 数据验证和 Schema 定义

### 认证授权
- **Buddy Sign** - JWT 签名和验证
- **Buddy Hashers** - 密码哈希

### 数据库
- **next.jdbc** - JDBC 数据库访问
- **HikariCP** - 连接池
- **HoneySQL** - SQL 查询构建器
- **Migratus** - 数据库迁移
- **PostgreSQL** - 数据库驱动

### 日志
- **Timbre** - 日志框架

### 开发工具
- **Integrant REPL** - REPL 驱动开发
- **Kaocha** - 测试框架
- **Ring Mock** - HTTP 请求模拟

## 项目结构

```
clojure-scaffold/
├── src/
│   ├── clj/myapp/          # 应用源代码
│   │   ├── core.clj        # 应用入口
│   │   ├── system.clj      # 系统配置
│   │   ├── config.clj      # 配置管理
│   │   ├── db/             # 数据库层
│   │   ├── handlers/       # 请求处理器
│   │   ├── middleware/     # 中间件
│   │   └── routes/         # 路由定义
│   ├── java/               # Java 源代码
│   └── resources/          # 资源文件
│       ├── logback.xml     # 日志配置
│       └── migrations/     # 数据库迁移脚本
├── test/clj/               # 测试代码
├── dev/                    # 开发环境配置
├── build/                  # 构建脚本
├── scripts/                # 工具脚本
└── deps.edn                # 依赖配置
```

## 快速开始

### 前置要求

- Java 11+
- Clojure CLI tools
- PostgreSQL

### 安装依赖

```bash
clojure -P
```

### 配置数据库

1. 创建数据库：
```bash
createdb myapp_dev
createdb myapp_test
```

2. 配置环境变量（可选，创建 `.env` 文件）：
```bash
DATABASE_URL=jdbc:postgresql://localhost:5432/myapp_dev
```

### 运行数据库迁移

```bash
clojure -M:dev -e "(require 'myapp.db.migrations) (myapp.db.migrations/migrate)"
```

### 启动开发服务器

```bash
clojure -M:dev
```

然后在 REPL 中：
```clojure
(go)      ; 启动系统
(reset)   ; 重启系统
(halt)    ; 停止系统
```

### 运行测试

```bash
clojure -M:test -m kaocha.runner
```

### 构建 Uberjar

```bash
clojure -T:build uber
```

### 运行生产版本

```bash
java -jar target/myapp-standalone.jar
```

## 功能特性

### ✅ 用户认证
- JWT Token 认证
- 密码加密存储
- 登录/注册接口

### ✅ 数据库集成
- PostgreSQL 连接池
- 数据库迁移管理
- HoneySQL 查询构建

### ✅ RESTful API
- Reitit 路由
- 请求/响应格式化
- 数据验证

### ✅ 开发体验
- REPL 驱动开发
- 热重载
- 完整的测试套件

## API 端点

### 认证
- `POST /api/auth/register` - 用户注册
- `POST /api/auth/login` - 用户登录

### 用户
- `GET /api/users` - 获取用户列表（需要认证）
- `GET /api/users/:id` - 获取用户详情（需要认证）

## 开发指南

### REPL 工作流

1. 启动 REPL：`clojure -M:dev`
2. 加载开发环境：`(require 'user)`
3. 启动系统：`(go)`
4. 修改代码后重启：`(reset)`

### 添加新的路由

1. 在 `src/clj/myapp/handlers/` 创建处理器
2. 在 `src/clj/myapp/routes/` 定义路由
3. 在 `src/clj/myapp/system.clj` 中注册路由

### 数据库迁移

创建新的迁移文件：
```bash
# 在 src/resources/migrations/ 目录下创建
# 格式：YYYYMMDDHHMMSS-description.up.sql
#      YYYYMMDDHHMMSS-description.down.sql
```

## 配置

应用配置通过 Aero 管理，支持环境变量和配置文件。

配置文件位置：`src/resources/config.edn`

## 测试

```bash
# 运行所有测试
clojure -M:test

# 运行特定测试
clojure -M:test --focus myapp.handlers.auth-test

# 监听模式
clojure -M:test --watch
```

## 部署

### Docker（待实现）

```bash
docker build -t myapp .
docker run -p 3000:3000 myapp
```

### 环境变量

- `PORT` - 服务器端口（默认：3000）
- `DATABASE_URL` - 数据库连接字符串
- `JWT_SECRET` - JWT 签名密钥

## 许可证

MIT

## 贡献

欢迎提交 Issue 和 Pull Request！
