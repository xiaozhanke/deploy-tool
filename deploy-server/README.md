# 管理端部署工具后端

一个基于 Spring Boot 的服务器部署管理工具后端，提供 SSH 连接、文件传输、命令执行等功能。

## 功能特性

- 🚀 **服务器管理**
  - 支持多服务器配置管理
  - 支持密码和密钥两种认证方式
  - 支持自定义 SSH 连接参数（算法、超时等）

- 🔐 **SSH 操作**
  - 支持创建和管理 SSH 会话
  - 支持交互式 Shell 操作
  - 支持执行远程命令
  - 支持文件传输

- 📝 **配置管理**
  - 使用 FreeMarker 模板引擎生成配置文件
  - 支持动态生成 Nginx 配置文件
  - 支持自定义模板配置

- 🛠 **技术特性**
  - 基于 Spring Boot 3.x
  - 使用 JPA 进行数据持久化
  - 集成 Swagger/OpenAPI 3.0 文档
  - 支持 WebSocket 实时通信
  - 使用 MapStruct 进行对象映射
  - 使用 FreeMarker 进行模板渲染

## 开发工具推荐

### IDE 推荐

强烈推荐使用 [IntelliJ IDEA](https://www.jetbrains.com/idea/) 作为开发 IDE，它提供了以下优势：

- 🚀 **智能代码补全**
  - 强大的代码分析和智能提示
  - 支持 Spring Boot 框架的智能感知
  - 自动补全 Lombok 注解

- 🔍 **调试功能**
  - 内置 Spring Boot 运行配置
  - 支持热重载（Hot Reload）
  - 可视化的断点调试

- 📦 **依赖管理**
  - 内置 Maven 支持
  - 依赖冲突检测
  - 自动更新依赖版本

- 🛠 **其他特性**
  - 内置 Git 支持
  - 代码格式化工具
  - 代码检查工具
  - 数据库工具
  - REST 客户端

### 插件推荐

建议安装以下 IDEA 插件以提升开发效率：

- **Lombok**: 支持 Lombok 注解的智能提示
- **MapStruct Support**: MapStruct 代码生成支持
- **POJO to JSON**: 快速转换 POJO 类生成 JSON 字符串

## 技术栈

- **后端框架**
  - Spring Boot 3.x
  - Spring Data JPA
  - Spring WebSocket
  - Spring Validation
  - Apache FreeMarker

- **数据库**
  - H2 Database

- **工具库**
  - Lombok
  - MapStruct
  - JSch (SSH 实现)
  - SpringDoc OpenAPI

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.6+
- Node.js 20.19+ (用于前端构建)

### 构建项目

```bash
# 进入项目目录
cd deploy-server

# 构建后端
mvn clean package

# 运行项目
mvn spring-boot:run
```

### 开发环境配置

1. 确保已安装 JDK 21 和 Maven
2. 配置 application-dev.yml 中的数据库连接信息
3. 使用 dev profile 运行项目：
   ```bash
   mvn spring-boot:run -Pdev
   ```

### API 文档

启动项目后，访问以下地址查看 API 文档：
- Swagger UI: http://localhost:6060/swagger-ui.html
- OpenAPI 文档: http://localhost:6060/v3/api-docs

## 项目结构

```
deploy-server/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com.xiaozhanke.deploy/
│   │   │       ├── common/         # 公共组件
│   │   │       ├── config/         # 配置类
│   │   │       ├── constant        # 常量类
│   │   │       ├── controller/     # 控制器
│   │   │       ├── enums/          # 枚举类
│   │   │       ├── exception/      # 异常处理类
│   │   │       ├── model/          # 数据模型
│   │   │       │   ├── dto/        # 数据传输对象
│   │   │       │   ├── entity/     # 实体类
│   │   │       │   ├── mapper/     # 实体类转换器
│   │   │       │   ├── request/    # 请求参数
│   │   │       │   └── vo/         # 视图对象
│   │   │       ├── repository/     # 数据访问层
│   │   │       ├── security/       # 安全认证授权处理
│   │   │       └── service/        # 业务逻辑层
│   │   └── resources/
│   │       ├── static/             # 静态资源目录
│   │       ├── templates/          # 模板目录
│   │       ├── application.yml     # 主配置文件
│   │       ├── application-dev.yml # 开发环境配置
│   │       └── application-pro.yml # 生产环境配置
│   └── test/                       # 测试代码
└── pom.xml                         # Maven 配置
```

## 开发指南

### 添加新功能

1. 在 `model` 包下创建相应的 DTO、Entity 和 VO 类
2. 在 `repository` 包下创建数据访问接口
3. 在 `service` 包下实现业务逻辑
4. 在 `controller` 包下创建 REST 接口
5. 使用 Swagger 注解添加 API 文档

### 代码规范

- 遵循阿里巴巴 Java 开发手册
- 使用 Lombok 简化代码
- 使用 MapStruct 进行对象映射
- 遵循 RESTful API 设计规范

## 部署

### 生产环境配置

1. 修改 `application-pro.yml` 中的配置
2. 使用 pro profile 构建项目：
   ```bash
   mvn clean package -Ppro
   ```
3. 运行生成的 jar 包：
   ```bash
   java -jar target/deploy-server-1.2.4.jar
   ```

## 常见问题解答（Q&A）

### 1. IDEA 中注解处理器不生效怎么办？

如果发现 Lombok、MapStruct 等注解处理器不生效，请按以下步骤检查设置：

1. 打开 IDEA 设置（Settings）
2. 导航到 `Build, Execution, Deployment` > `Compiler` > `Annotation Processors`
3. 确保以下选项已正确配置：
   - ✅ 勾选 `Enable annotation processing`
   - ✅ 选择 `Obtain processors from project classpath`
   - 确保 `Store generated sources relative to:` 选择 `Module content root`
   - 生成的源代码目录应为：
     - Production: `target/generated-sources/annotations`
     - Test: `target/generated-test-sources/test-annotations`
![注解处理器设置示例](/docs/images/idea-settings-annotation-processors.png)

如果配置完成后仍然不生效，请尝试：
1. 清理项目（`mvn clean`）
2. 重新构建项目
3. 重启 IDEA

### 2. 如何确认注解处理器工作正常？

可以通过以下方式确认：

1. 检查 `target/generated-sources/annotations` 目录是否生成了相应的文件
2. 对于 MapStruct，应该能看到生成的 `*Impl.java` 文件
3. 对于 Lombok，确认能够正常使用 `@Getter`、`@Setter` 等注解，且不会报红
