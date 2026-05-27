# Docker 环境

本目录托管 deploy-tool 开发期的容器化依赖。

## RocketMQ 单机开发环境

deploy-tool MQ 模块(详见 [MQ模块设计方案.md](../MQ模块设计方案.md))依赖 RocketMQ 5.x。本目录提供一键起 NameServer + Broker + Dashboard 的 Compose 文件。

### 启动

```bash
cd docker
docker compose -f docker-compose-mq.yml up -d
```

### 端口约定

| 服务 | 端口 | 用途 |
|---|---|---|
| NameServer | 9876 | RocketMQ 路由发现 |
| Broker | 10909 / 10911 / 10912 | VIP 通道 / 消息收发 / HA |
| Dashboard | 8180 | Web 控制台,浏览器访问 <http://localhost:8180> |

### 验证

启动后访问 dashboard,在「集群」页能看到 broker-a 注册即代表健康。

应用层(deploy-server)通过 `application-dev.yml` 里的 `rocketmq.name-server: 127.0.0.1:9876` 连过来,topic 在首次发送消息时自动创建(`autoCreateTopicEnable=true`)。

### 停止与清理

```bash
docker compose -f docker-compose-mq.yml down          # 停止容器
docker compose -f docker-compose-mq.yml down -v       # 顺带删卷(磁盘上的消息也清掉)
```

### 注意

- [broker.conf](rocketmq/broker.conf) 里 `brokerIP1 = 127.0.0.1` 是为了让宿主机 IDE 连接 broker 时拿到正确地址,**仅适合单机开发**;生产部署需改成真实节点 IP
- `autoCreateTopicEnable = true` 也只用于开发,生产环境要禁用,topic 由 admin 命令显式创建
- `defaultTopicQueueNums = 8` 给顺序消息按 `deploymentRecordId` 分队列(见 [ADR-0001](../docs/adr/0001-ordering-key-deployment-record.md))预留足够空间
