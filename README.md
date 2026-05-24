# 管理端部署工具

一个基于 Spring Boot + Vue3 的现代化服务器部署管理工具，提供直观的 Web 界面，支持服务器管理、SSH 操作、文件传输等功能。

## 项目介绍

本项目是一个完整的服务器部署管理解决方案，包含以下核心功能：

- 🖥️ **服务器管理**
  - 多服务器配置管理
  - 支持密码和密钥认证
  - 自定义 SSH 连接参数

- 🔧 **远程操作**
  - 在线 SSH 终端
  - 文件上传下载
  - 命令批量执行

- 📝 **配置管理**
  - 配置文件模板
  - Nginx 配置生成
  - 自定义模板配置

## 项目结构

```
deploy-tool/
├── deploy-server/  # Spring Boot 后端服务
└── deploy-web/     # Vue3 前端界面
```

## 技术选型

- **后端**: Spring Boot 3.x + JPA + WebSocket
- **前端**: Vue 3 + TypeScript + Element Plus
- **数据库**: H2 Database

## 快速开始

详细的开发和部署指南请参考各子项目的 README：

- [后端开发指南](deploy-server/README.md)
- [前端开发指南](deploy-web/README.md)

### 开发建议

- IDEA 工程根目录建议打开 `/deploy-tool`
- VSCode 工程根目录建议打开 `/deploy-tool/deploy-web`
- Git 在工程根目录 `/deploy-tool` 提交

## 开发环境

- JDK 21+
- Node.js 20.19+
- Maven 3.6+
