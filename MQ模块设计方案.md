# deploy-tool MQ 模块设计方案

> **创建日期**：2026-05-23
> **目的**：在 deploy-tool（独立开发项目）中引入 RocketMQ + Kafka 双引擎，弥补简历上"无 MQ 生产实战"短板;通过 6 个生产级场景覆盖 Java 后端面试 MQ 高频考点。
> **预期效果**：简历可写"MQ 深度实践";目标薪资从 26-32K 拉到 30-38K（互联网中厂可面）。

---

## 一、中间件选型:**RocketMQ 为主 + Kafka 为辅**(双引擎)

| 维度 | RocketMQ | Kafka |
|---|---|---|
| 用途 | **业务消息**(事务、顺序、延迟、重试) | **数据流**(操作日志、审计采集) |
| 面试热点 | 事务消息两阶段、顺序消息、DLQ | ISR、副本、acks、Rebalance |
| 简历叙事 | 金融/银行偏爱(与背景契合) | 互联网通用(覆盖中厂面试) |
| 学习成本 | 中(Spring 集成好) | 中(Spring Kafka 成熟) |

### 为什么不只用一个

- 只用 RocketMQ → 银行系够用,但被互联网面试官问"Kafka 用过没"会减分
- 只用 Kafka → 没了事务消息这个金融场景杀手特性
- **两个都有 → 能讲"业务消息 vs 数据流"的选型对比**,这是 4 年经验该有的判断力

### 为什么不用 Spring Cloud Stream 抽象层

- Stream 抽象了底层细节,面试问"事务消息半消息流程"反而不好答
- 直接用 `rocketmq-spring-boot-starter` + `spring-kafka` 原生 SDK,**面试细节抓手更多**

---

## 二、6 个业务场景(每个都对应面试考点)

### 场景 1:部署作业异步化 + 事务消息(RocketMQ)⭐⭐⭐⭐⭐

**业务**:用户点"部署/重启/停止/更新"按钮 → 新建一份**部署作业**入库 + 异步执行远程 SSH 命令(部署记录已预先存在,本次按钮点击不改它)

**痛点**:当前是同步执行(卡住 HTTP 线程几十秒),失败重试也没法做

**HTTP 入口防重**:Controller 接收 `(deploymentRecordId, jobType, clientRequestId)` 三元组,在创建作业前用 `(deployment_record_id, job_type, client_request_id)` 唯一索引拦截重复;命中则直接返回已存在的 jobId,**不**进入本地事务、**不**发新消息。

**MQ 改造**:
- 发**事务消息**保证"**部署作业**入库"与"消息发送"的原子性
- `executeLocalTransaction` 回调**仅**做 `INSERT INTO deployment_job (id, type, deployment_record_id, status='PENDING', client_request_id, ...)` 一行;SSH 远程命令**不**在本地事务内(本地事务必须秒级可判定,否则破坏回查机制)
- `checkLocalTransaction` 回查逻辑:`SELECT FROM deployment_job WHERE id=?` 存在则 COMMIT、不存在则 ROLLBACK
- Consumer 拿到消息后由 ADR-0002 的 CAS UPDATE 占据作业,再执行 JSch 远程命令
- 部署记录的运行态投影(`running`/`processId`/`lastStartTime`)由消费者在作业成功后顺手刷新,**不**在本地事务内
- 状态变化通过 WebSocket 推前端(已有 STOMP 通道)

**三层防重**(对应面试抓手):
1. 前端按钮防抖(第一道)
2. HTTP 唯一索引拦截重复 clientRequestId(第二道)
3. 消费者 CAS UPDATE 占据 jobId(第三道,见 ADR-0002)

**面试考点**:
- 事务消息的 half message / commit / rollback / 回查机制
- 本地事务执行器(`RocketMQLocalTransactionListener`)怎么写,为什么必须秒级可判定
- 为什么 RocketMQ 事务消息比 Seata + TCC 轻量
- 入口 + 本地事务 + 消费端的三层防重,各自挡的是什么场景

---

### 场景 2:单部署记录作业串行化 + 顺序消息(RocketMQ)⭐⭐⭐⭐

**业务**:对同一份**部署记录**连续触发 STOP/START/UPDATE/RESTART 必须严格串行(避免 STOP 与 START 交错、UPDATE 重叠);不同部署记录之间并发,哪怕落在同一台物理机。

**MQ 改造**:
- `MessageQueueSelector` 用 `deploymentRecordId.hashCode() % queueCount` 选 queue
- 同一份部署记录的所有作业进同一队列 → 串行消费
- 不同部署记录(包括同机不同应用)的作业分散到不同队列 → 并行
- 见 [ADR 0001](docs/adr/0001-ordering-key-deployment-record.md):为什么不选 `serverId` 或 `(serverId, port)`

**面试考点**:
- 顺序消息原理(partial order vs total order)
- 为什么 Kafka 也能做但不如 RocketMQ 灵活
- 顺序消息消费失败的处理(不能 ACK 进下一条,会卡死)
- **顺序键选型抓手**:为什么不按 `serverId` 排队、为什么不按 `(serverId, port)`、不可变 id 与可变配置的取舍

---

### 场景 3:定时部署 + 延迟消息 + 取消语义(RocketMQ)⭐⭐⭐

**业务**:用户配置"凌晨 2 点自动部署"或"5 分钟后自动重启",且**支持随时撤销已配置但尚未执行的延迟作业**。

**MQ 改造**:
- 用 RocketMQ 的 `delayLevel`(18 个等级,1s~2h)发延迟消息
- 长延迟用"短延迟+重发"模式实现任意时长(每次接力前消费端先查 status,CANCELLED 则链条终止)
- **取消接口**:HTTP 端把 `deployment_job.status` 从 PENDING 转为 CANCELLED;延迟消息到期照常触达,但消费首行 CAS UPDATE 不命中直接 ACK——"消息不可靠撤回 + 业务可靠仲裁"(详见 ADR-0004)
- UI 的"待执行作业"列表展示 PENDING 延迟作业 + "撤销"按钮

**面试考点**:
- RocketMQ 延迟消息原理(时间轮 + delay topic)
- vs Kafka 没原生延迟(要外部组件如 kafka-delay-queue)
- vs 数据库轮询(性能差距)
- **延迟消息的取消语义**:为什么 RocketMQ 不支持撤回、应用层怎么实现取消、CAS UPDATE 如何作为撤销与触达的最终仲裁(详见 ADR-0004)
- 长延迟"短延迟+重发"接力链与 CANCELLED 状态的协作

---

### 场景 4:操作审计日志 + Kafka 高吞吐 + 非事务路线 ⭐⭐⭐⭐

**业务**:SSH 命令执行记录、文件传输记录、登录登出 → 全部异步采集,审计走"最终可观测"而**不**走事务一致性。

**MQ 改造**:
- AOP 双切面:`@AfterReturning` + `@AfterThrowing`,审计消息携带 `outcome=SUCCESS/FAILURE`(失败的安全事件也要记,单 `@AfterReturning` 会漏)
- `KafkaTemplate.send(...).get(timeout=3s)` 同步发送 + Producer `acks=all` + `min.insync.replicas=2` + `retries=3`
- 发送失败**不回滚业务**,落本地兜底文件 + 告警;Kafka 恢复后由独立 job 批量回放(详见 ADR-0005)
- Consumer Group 并发消费 → 写入 MySQL 审计表
- Topic 分 3 个 partition,演示并发消费

**关键决策**:**不**用本地消息表事务——审计的合规要求是"最终可观测"而非"原子",本地消息表会成为瓶颈、削弱 Kafka 高吞吐叙事(详见 ADR-0005)。

**面试考点**:
- Kafka 高吞吐原理(顺序写 + page cache + 零拷贝 sendfile)
- Producer `acks=all` + `min.insync.replicas=2`
- 消费位移提交策略(auto vs manual,At-Least-Once)
- Consumer Group + Partition Rebalance
- **审计为什么不用事务**:RocketMQ 业务消息要事务、Kafka 数据流不要事务,场景选型差异(详见 ADR-0005)
- AOP 双切面 vs 单切面的审计语义差异

---

### 场景 5:失败重试 + 死信队列(混合策略,放弃 RocketMQ 内置重试)⭐⭐⭐⭐

**业务**:部署作业失败时按故障性质分流——瞬时故障短重试挽救,终态失败立即 DLQ;**保留顺序消息的顺序保证**(见 ADR-0001)。

**关键决策**:**不使用** RocketMQ 内置的 `maxReconsumeTimes` + 自动 DLQ(`%DLQ%consumerGroup`),因为内置重试机制会把失败消息丢到 retry topic 延迟投回,与顺序消息互斥(详见 ADR-0003)。

**MQ 改造**:
- consumer 区分异常类型:
  - 基础设施级(`SshTransientException` 等) → 在同一条消息的消费动作内做 2-3 次短同步重试(小退避),对 MQ 总是 ACK 成功 → **顺序不破**
  - 业务级 / 短重试用尽(`JobFailureException` / 重试次数耗尽) → 数据库 `status='FAILED'` + 写错误信息 + ACK 原消息(让 queue 继续) + 显式 `rocketMQTemplate.syncSend("%DLQ%deploy-job", ...)`
- DLQ Topic 用业务自定义名 `%DLQ%deploy-job`,**不**用 RocketMQ 自动 DLQ 名
- 前端 "死信查看" 页面 + 手动 retry 按钮(retry = **新建一份新 jobId 的作业**走 HTTP 入口,不是把 DLQ 消息再投回原 Topic——后者破顺序)

**面试考点**:
- 为什么放弃 RocketMQ 内置 `maxReconsumeTimes`:三段(机制层 retry topic 延迟投回破顺序 / 业务层不区分故障性质 / 运维层 DLQ 时机由应用决定更可观测)
- 顺序消息消费失败的几种处理方案对比:阻塞 queue / 立即 DLQ / 应用层混合(本项目选)
- **消费幂等**怎么做:为什么用业务键 `jobId` + CAS UPDATE,**不**用 `messageId` 去重表;事务消息 half/commit、producer 重试、broker 重投如何让 messageId 漂移(详见 ADR-0002)
- 自定义 DLQ Topic vs RocketMQ 自动 DLQ(`%DLQ%consumerGroup`)的差异
- 手动 retry 为什么要"新建作业"而不是"复投死信"——再讲一遍顺序保证

---

### 场景 6:配置广播 + 集群消费 vs 广播消费(RocketMQ)⭐⭐⭐

**业务**:管理员改了"全局 SSH 超时时间" → 所有实例的本地配置都要更新

**MQ 改造**:
- 配置变更 Topic 用**广播消费** `MessageModel.BROADCASTING`
- 部署作业 Topic 用**集群消费**做对比

**面试考点**:
- 集群消费(消费位移在 Broker) vs 广播消费(位移在本地)的实现差异
- 广播模式不能用消费失败重试
- 实际项目里广播怎么用(多级缓存失效、配置中心降级)

---

## 三、模块架构设计

```
deploy-server/
├── src/main/java/com/xiaozhanke/deploy/
│   ├── messaging/                         # 新增模块
│   │   ├── config/
│   │   │   ├── RocketMQConfig.java        # Producer/Consumer 配置
│   │   │   └── KafkaConfig.java           # Producer/Consumer 配置
│   │   ├── producer/
│   │   │   ├── DeploymentMQProducer.java  # 事务/顺序/延迟消息生产
│   │   │   └── AuditLogProducer.java      # Kafka 审计日志生产
│   │   ├── consumer/
│   │   │   ├── DeploymentConsumer.java    # 部署作业消费 + DLQ 处理
│   │   │   ├── ConfigBroadcastConsumer.java  # 广播消费
│   │   │   └── AuditLogConsumer.java      # Kafka 审计日志消费
│   │   ├── transaction/
│   │   │   └── DeploymentTransactionListener.java  # 事务消息回查
│   │   ├── selector/
│   │   │   └── DeploymentRecordQueueSelector.java # 顺序消息 queue 选择器(按 deploymentRecordId)
│   │   ├── idempotent/
│   │   │   └── JobAcquisitionService.java  # 消费幂等(按 jobId 的 CAS UPDATE,见 ADR-0002)
│   │   └── dto/
│   │       ├── DeploymentJobMessage.java
│   │       ├── ConfigChangeMessage.java
│   │       └── AuditLogMessage.java
│   ├── controller/
│   │   ├── DeploymentController.java      # 改造:同步执行 → 发 MQ
│   │   └── MQMonitorController.java       # 新增:DLQ 查看/重投
│   └── service/
│       └── DeploymentService.java         # 改造:异步化
├── docker/
│   ├── docker-compose-mq.yml              # RocketMQ + Kafka 一键启动
│   ├── rocketmq/
│   │   ├── broker.conf
│   │   └── plain_acl.yml
│   └── kafka/
│       └── server.properties
└── pom.xml                                # 新增依赖
```

### pom.xml 新增依赖

```xml
<!-- RocketMQ -->
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-spring-boot-starter</artifactId>
    <version>2.3.1</version>
</dependency>
<!-- Kafka -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

---

## 四、Docker Compose 一键启动(开发环境)

```yaml
# docker/docker-compose-mq.yml
version: '3.8'
services:
  rmqnamesrv:        # NameServer
  rmqbroker:         # Broker(单节点开发够用)
  rmqconsole:        # Web 控制台 :8180

  kafka:             # KRaft 模式,无需 ZK
  kafka-ui:          # provectuslabs/kafka-ui :8080
```

一键 `docker-compose up -d`,团队/面试官都能本地复现。

---

## 五、简历话术(直接写进 `deploy-tool/项目经历总结.md`)

> **6. 消息中间件深度实践**
>
> 引入 **RocketMQ + Kafka 双引擎**,演示业务消息与数据流场景的选型对比:
> - **RocketMQ 事务消息**:解决"部署作业入库 + 远程命令执行"的最终一致性,本地事务只做一行 INSERT、SSH 在消费端执行;实现 `RocketMQLocalTransactionListener` 处理半消息回查
> - **RocketMQ 顺序消息**:基于 `MessageQueueSelector` 按 `deploymentRecordId` 哈希分队列,保证同一份部署记录的连续作业串行、不同部署记录并发(即便同机);决策详见 ADR-0001
> - **RocketMQ 延迟消息**:实现定时部署、定时健康检查,对比 DB 轮询性能提升数量级
> - **死信队列 + 消费幂等**:消费失败自动重试(指数退避 3 次)后进 DLQ,前端可查看与重投;**双层防重**——HTTP 入口用 `(deploymentRecordId, jobType, clientRequestId)` 唯一索引挡前端重复点击,消费者用 `UPDATE ... WHERE status='PENDING'` CAS 占据作业,**不**依赖 messageId 去重(避开事务消息半提交/重试导致的 messageId 漂移)
> - **广播消费**:配置变更实时同步到所有实例
> - **Kafka 高吞吐审计**:操作日志异步采集,单 Topic 3 partition + Consumer Group 并发消费,`acks=all` 保证不丢

---

## 六、工作量预估(独立开发)

| 阶段 | 工作量 | 内容 |
|---|---|---|
| 1. 环境搭建 | 0.5 天 | docker-compose 起 RocketMQ + Kafka |
| 2. 事务消息 + 顺序键(场景 1 + 2) | 1.5 天 | Producer + Listener + Consumer + DeploymentRecordQueueSelector + JobAcquisitionService + 改 DeploymentController(新增 jobs 接口、旧接口 @Deprecated) |
| 4. 延迟消息(场景 3) | 0.5 天 | 定时任务配置 + delayLevel |
| 5. Kafka 审计(场景 4) | 1 天 | AOP 拦截 + Producer + Consumer + 落库 |
| 6. DLQ + 幂等(场景 5) | 1 天 | 重试配置 + DLQ Consumer + 幂等表 + 前端 DLQ 页面 |
| 7. 广播消费(场景 6) | 0.5 天 | 广播 Topic + Consumer |
| 8. 联调 + 文档 | 1 天 | 集成测试 + 更新 项目技术分析报告.md |
| **合计** | **~7 天** | 业余时间 2-3 周 |

---

## 七、风险与权衡

| 风险点 | 应对 |
|---|---|
| 面试官问"MySQL 单实例 + 单机 MQ 不是真生产" | 答:"**作品集项目演示技术能力**,生产环境会用 MySQL 主从 + RocketMQ 集群(NameServer 2+,Broker 主从)" |
| 单机 MQ 看不出集群特性 | docker-compose 里**起 2 个 Broker**(主从模式),多花 1 天但能讲集群同步 |
| 没有真实流量看不出吞吐 | 写个 JMeter / `wrk` 压测脚本,README 里贴数字(5K TPS 演示,不夸大) |
| 简历"金融背景 + Kafka"会让人觉得偏 | 用一句"业务消息用 RocketMQ,日志流用 Kafka"的选型理由,**反而成为加分点** |

---

## 八、要不要两个都做?

**建议**:**两个都做**,但**优先级分层**——

- **必做(5 天)**:场景 1、2、5 + Kafka 场景 4
  - 覆盖事务消息、顺序消息、DLQ、幂等、Kafka 副本/位移——**面试主流问题全覆盖**
- **加分(2 天)**:场景 3、6
  - 延迟消息、广播消费——锦上添花
- **可选**:自定义 MQ 监控大屏(接入 piop-ui 的 ECharts),把作品集再串一下

---

## 九、面试可讲点速查表(开发完成后填充)

> 完成开发后,把每个场景里"我具体怎么做的、踩过什么坑、为什么这么选"补充到下方,作为面试自查清单。

### RocketMQ
- [ ] 事务消息两阶段提交完整流程(half → local txn → commit/rollback → check)
- [ ] `RocketMQLocalTransactionListener` 的 `executeLocalTransaction` 和 `checkLocalTransaction` 分别什么时候调用
- [ ] 顺序消息的 `MessageQueueSelector` 怎么实现负载均衡 + 顺序
- [ ] 延迟消息的 18 个 delayLevel 分别是什么,长延迟怎么实现
- [ ] DLQ Topic 名字规则(`%DLQ%consumerGroup`),怎么消费 DLQ
- [ ] 消费幂等的几种实现(messageId 去重 / 业务唯一键 / CAS 状态机),为什么本项目选最后一种
- [ ] 集群消费 vs 广播消费的位移存储位置差异
- [ ] Push 模式 vs Pull 模式
- [ ] NameServer 的作用,为什么不用 ZK
- [ ] Broker 主从同步(同步刷盘 vs 异步刷盘)

### Kafka
- [ ] Topic / Partition / Replica / ISR 概念
- [ ] `acks=0/1/all` 的语义,数据丢失风险
- [ ] `min.insync.replicas` 的作用
- [ ] 消费位移提交策略(`enable.auto.commit` vs 手动提交)
- [ ] At-Least-Once / At-Most-Once / Exactly-Once 三种语义怎么实现
- [ ] Consumer Group + Rebalance 触发条件 + Sticky 策略
- [ ] 顺序写磁盘 + page cache + sendfile 零拷贝怎么实现高吞吐
- [ ] KRaft 模式 vs ZK 模式的区别

### 选型对比
- [ ] 为什么 deploy-tool 业务消息选 RocketMQ,日志流选 Kafka
- [ ] RocketMQ 事务消息 vs Seata + TCC vs 本地消息表
- [ ] RocketMQ 顺序消息 vs Kafka 单分区
- [ ] 都用过的场景下,日常 OPS 哪个更省心

---

## 十、后续开发路径

1. **第 1 周**:完成 Phase 1(场景 1 事务消息 + Docker 环境)→ 提交 commit
2. **第 2 周**:完成 Phase 2(场景 2/5 顺序消息 + DLQ)+ Kafka 场景 4
3. **第 3 周**:完成场景 3/6(延迟 + 广播)+ 压测 + 文档
4. **完成后**:
   - 更新 `deploy-tool/项目经历总结.md` 加上"MQ 深度实践"段落
   - 更新 `deploy-tool/项目技术分析报告.md` 加上 MQ 章节
   - 更新顶层 `interview/CLAUDE.md` 的"跨工程技术故事"加上 MQ 主题(可以串 new_posp 的"评估过未引入" + deploy-tool 的"独立设计")
   - 在 GitHub 上把 deploy-tool 仓库整理一下,README 加上"MQ 模块"的截图/动图

---

## 参考

- deploy-tool 现有架构详见 `deploy-tool/CLAUDE.md`
