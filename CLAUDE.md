# CLAUDE.md

此文件为 Claude Code (claude.ai/code) 在本仓库工作时提供指引。

## 仓库结构

两个并列子工程位于同一 git 根目录下，所有 commit 均从根目录提交：

- `deploy-server/` —— Spring Boot 3.5.14 后端，Java 21，Maven（artifact `com.xiaozhanke:deploy-server`，根包 `com.xiaozhanke.deploy`）
- `deploy-web/` —— Vue 3 + Vite + TypeScript 前端
- `database/` —— H2 文件型数据库（`./database/deploy.mv.db`），已纳入 git
- `docs/`、`files/`、`logs/` —— 运行时/资产目录
- `MQ模块设计方案.md` —— 规划中的 RocketMQ + Kafka 模块设计稿（尚未实现）

两个子工程开发期松耦合，但生产环境产出**单一 Spring Boot jar**：后端 Maven 构建会通过 `exec-maven-plugin`（绑定在 `generate-resources` 阶段）调用 `npm run build` 触发前端构建，并把 `deploy-web/dist` 拷贝到 `deploy-server/src/main/resources/static/`。`mvn clean` 会清空该 `static/` 目录。**不要手工编辑 `static/` 下任何内容。**

## 常用命令

### 后端（在 `deploy-server/` 目录下运行）
```bash
mvn spring-boot:run            # dev profile 默认激活
mvn clean package              # 构建前端 + 打成 jar（dev profile）
mvn clean package -Ppro        # 生产 profile
mvn test                       # JUnit（基于 spring-boot-starter-test）
mvn -Dtest=ClassName#method test   # 跑单个测试
```

Maven 中 `dev` profile 标了 `activeByDefault=true`，会设置 `spring.profiles.active=dev`。生产构建用 `-Ppro`。

### 前端（在 `deploy-web/` 目录下运行）
```bash
npm install
npm run dev          # Vite 开发服务器：https://localhost:5173/ui（basic-ssl 提供 HTTPS）
npm run build        # type-check + vite build → dist/
npm run lint         # eslint --fix
npm run format       # prettier --write src/
npm run type-check   # vue-tsc --build（仅类型检查不产物）
```

`npm run build` 通过 `npm-run-all2` 并行跑 `type-check` 和 `build-only`。

⚠️ **`npm run lint`（eslint --fix）有一个已知坑**：会主动删除"看起来多余但其实必要"的类型断言（如 `as '' | 'small' | 'default' | 'large'` 这种为了保留字面量类型而加的断言），删掉后 vue-tsc 会因为类型推断变宽而报错。后续使用 lint 前注意这点。

## 运行架构

### 端口与 URL
- API: `https://localhost:6060`（HTTPS 通过 `keystore.p12`，密码 `changeit`，alias `deploy`）
- 前端开发：`https://localhost:5173/ui`（基础路径 `/ui`，由 `VITE_BASE_URL` 设置）
- Swagger: `https://localhost:6060/swagger-ui.html`
- OpenAPI JSON: `https://localhost:6060/v3/api-docs`

### 开发模式请求流向
Vite 代理两个前缀到后端 API（见 `vite.config.ts`）：
- `${VITE_API_ROOT}`（默认 `/api`） → `https://localhost:6060`
- `/.well-known` → `https://localhost:6060`（OIDC discovery）

WebSocket 直连后端 `wss://localhost:6060/websocket`，不走代理 —— 见 `.env.development`。

### 安全模型
- Spring Authorization Server + Resource Server（OAuth2 / OIDC）**同进程内**部署，API 自己签发 token
- OIDC 客户端 ID `oidc-client`，公开客户端（PKCE，`client-authentication-methods: none`），回调地址 `/ui/login/callback`
- 前端使用 `oidc-client-ts`
- WebSocket 安全依赖 `spring-security-messaging`；Java 包 `security/` 下有 `config/`、`exception/`、`token/`、`user/` 四个子目录

### 后端包结构（`com.xiaozhanke.deploy`）
- `controller/` —— REST 接口（Auth、Config、Deployment、File、PlatformRole、PlatformUser、Server、Ssh、WebSocketSsh、Test）
- `service/` —— 业务逻辑（ConfigService、DeploymentService、FileStorageService、PlatformRoleService、PlatformUserService、ServerService、SshService）
- `core/ssh/` —— 基于 JSch 的 SSH 会话管理（用的是 `com.github.mwiede:jsch`，**不是**原始的 jcraft fork）
- `core/websocket/` —— 浏览器端 SSH 终端的 STOMP / WebSocket 通道
- `model/{dto,entity,mapper,request,vo}` —— MapStruct 在 entity / DTO / VO 之间做映射
- `security/`、`filter/`、`interceptor/`、`validation/`、`exception/`、`config/`、`constant/`、`enums/`、`util/`

### 持久化
- H2 文件型数据库位于 `./database/deploy`（相对于工作目录）。`mv.db` 文件**已纳入 git**，作为种子开发数据库 —— 提交前注意检查
- JPA：`ddl-auto: update`、`open-in-view: false`
- 构建期启用 Hibernate 字节码增强（`hibernate-enhance-maven-plugin`：lazy init、dirty tracking、association management）。不要在不理解后果的情况下禁用 —— commit `2047dfd` 就是修一个因为这套增强而暴露出来的 JPA 审计 bug
- 可选的 SQL 种子初始化在 `classpath:/sql/data.sql`（当前在 `application-dev.yml` 里被注释掉了）

### 前端栈要点
- Element Plus 通过 `unplugin-vue-components` + `unplugin-auto-import` 自动导入；类型分别生成在 `types/auto-imports.d.ts` 和 `types/components.d.ts`
- Element Plus 样式以 Sass 形式引入（`importStyle: 'sass'`），所有 SCSS 文件会自动注入 `@use "@/styles/element/index.scss" as *`
- `@` 别名指向 `src/`
- SSH 终端 UI 用 `@xterm/xterm`；WebSocket 用 `@stomp/stompjs`；配置编辑器用 `codemirror`（yaml 模式）
- 命名约定：组件文件用 PascalCase（如 `UserCard.vue`）、模板里用 kebab-case（如 `<user-card />`）、formatter 工具函数用 camelCase

## 注解处理器（容易踩坑）

`pom.xml` 在 `annotationProcessorPaths` 里串联了 MapStruct + Lombok + `lombok-mapstruct-binding`。如果 IDE 编译失败或者找不到生成的 `*Impl.java`，去 IntelliJ 里检查：*Build, Execution, Deployment → Compiler → Annotation Processors* —— 启用注解处理，并选 "Obtain processors from project classpath"。改完之后跑一次 `mvn clean` 再 rebuild。

## 版本号

后端版本（`pom.xml`）、前端版本（`package.json`）和 `application.yml` 里的 `spring.application.version` 始终保持一致（当前 `1.2.4`）。版本变更时三处一起改。

## 从最近提交历史观察到的约定

- Commit 用中文的 Conventional Commits 风格（`feat:`、`fix:`、`refactor:`、`build:`）
- README 三个文件（根、server、web）都是中文，编辑时跟随
- 后端遵循阿里巴巴 Java 规约；用 Lombok + MapStruct，实体不直接暴露给上层，走 DTO/VO 隔离
