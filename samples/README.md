# samples — deploy-tool 部署测试用的目标产物

本目录托管两个**最简化**的应用,用作 deploy-tool 端到端测试的部署目标:

| 子项目 | 类型 | 产物 | 部署目的 |
|---|---|---|---|
| [sample-app-backend](sample-app-backend/) | Spring Boot 3.5 / Java 17 | `target/sample-app-backend-1.0.0.jar` | 通过 SSH 推送到部署目标容器,验证 START/STOP/RESTART/UPDATE 作业 |
| [sample-app-frontend](sample-app-frontend/) | Vue 3 + Vite | `dist/` (可打包成 zip) | 通过 SFTP 推送并解压到 nginx 站点目录 |

部署目标本身是 [docker/Dockerfile](../docker/Dockerfile)(debian + sshd + JRE 17 + nginx + supervisord)的测试容器,启动后暴露 22 / 80 / 8080。

## 后端 sample-app-backend

```bash
cd sample-app-backend
mvn -q clean package
ls target/sample-app-backend-1.0.0.jar
```

启动:`java -jar sample-app-backend-1.0.0.jar --server.port=8080 --spring.profiles.active=test`

探活:
- `GET /hello` —— 返回 `{app, version, port, profile, bootTime}` 的 JSON
- `GET /actuator/health` —— Spring Boot Actuator 标准探活

## 前端 sample-app-frontend

```bash
cd sample-app-frontend
npm install
npm run build
ls dist/
```

产物可打成 zip:`cd dist && zip -r ../sample-app-frontend-1.0.0.zip .`

部署:deploy-tool 把 zip 上传到目标机后 `unzip -o sample-app-frontend-1.0.0.zip` 到 nginx 站点目录(默认 `/var/www/html`)。

## 在 deploy-tool 里关联这两个产物

1. 起部署目标容器:`docker build -t deploy-target docker/ && docker run -d -p 2222:22 -p 8080:8080 -p 80:80 --name deploy-target deploy-target`
2. 在 deploy-tool Web 控制台依次创建:
   - **ServerRecord**:host=`127.0.0.1` port=`2222` username=`root` password=`root`(或 `deploy` / `deploy`)
   - **FileRecord(后端)**:上传 `sample-app-backend-1.0.0.jar`
   - **FileRecord(前端)**:上传 `sample-app-frontend-1.0.0.zip`
   - **DeploymentRecord(后端)**:关联 server + backend file,deploymentPath=`/opt/sample-app`,port=`8080`,applicationType=`BACKEND`
   - **DeploymentRecord(前端)**:关联 server + frontend file,deploymentPath=`/var/www/html`,applicationType=`FRONTEND`
3. POST `/deployments/{id}/jobs` body `{"jobType":"START","clientRequestId":"<UUID>"}` 触发部署作业(详见 [MQ模块设计方案.md](../MQ模块设计方案.md) 场景 1)
