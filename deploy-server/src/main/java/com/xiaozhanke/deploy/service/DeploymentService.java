package com.xiaozhanke.deploy.service;

import com.xiaozhanke.deploy.enums.ApplicationTypeEnum;
import com.xiaozhanke.deploy.enums.DeploymentStatusEnum;
import com.xiaozhanke.deploy.exception.BusinessException;
import com.xiaozhanke.deploy.exception.InvalidOperationException;
import com.xiaozhanke.deploy.exception.ResourceNotFoundException;
import com.xiaozhanke.deploy.model.dto.ServerRecordDto;
import com.xiaozhanke.deploy.model.entity.DeploymentRecord;
import com.xiaozhanke.deploy.model.entity.FileRecord;
import com.xiaozhanke.deploy.model.entity.ServerRecord;
import com.xiaozhanke.deploy.model.mapper.DeploymentRecordPoVoMapper;
import com.xiaozhanke.deploy.model.request.DeploymentParams;
import com.xiaozhanke.deploy.model.response.PageResult;
import com.xiaozhanke.deploy.model.vo.DeploymentRecordVo;
import com.xiaozhanke.deploy.repository.DeploymentRecordRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 部署服务类
 *
 * @author xiaozhanke
 */
@Slf4j
@Service
public class DeploymentService {

    private final DeploymentRecordRepository deploymentRecordRepository;
    private final DeploymentRecordPoVoMapper deploymentRecordPoVoMapper;
    private final SshService sshService;
    private final ServerService serverService;
    private final FileStorageService fileStorageService;

    public DeploymentService(DeploymentRecordRepository deploymentRecordRepository,
                             DeploymentRecordPoVoMapper deploymentRecordPoVoMapper,
                             SshService sshService,
                             ServerService serverService,
                             FileStorageService fileStorageService) {
        this.deploymentRecordRepository = deploymentRecordRepository;
        this.deploymentRecordPoVoMapper = deploymentRecordPoVoMapper;
        this.sshService = sshService;
        this.serverService = serverService;
        this.fileStorageService = fileStorageService;
    }

    /**
     * 创建部署记录
     *
     * @param params 部署参数
     * @return 保存后的部署记录信息
     */
    @Transactional
    public DeploymentRecordVo createDeployment(DeploymentParams params) {
        ServerRecord serverRecord = serverService.getServer(params.getServerRecordId());
        FileRecord fileRecord = fileStorageService.getFileRecord(params.getFileRecordId());

        DeploymentRecord deployment = new DeploymentRecord()
                .setServerRecord(serverRecord)
                .setFileRecord(fileRecord)
                .setApplicationType(params.getApplicationType())
                .setDeploymentPath(params.getDeploymentPath())
                .setDeploymentConfigPath(params.getDeploymentConfigPath())
                .setPort(params.getPort())
                .setProgramArgs(params.getProgramArgs())
                .setActiveProfiles(params.getActiveProfiles())
                .setStatus(params.getStatus())
                .setErrorMessage(params.getErrorMessage())
                .setDeployTime(LocalDateTime.now())
                .setLastStartTime(params.getLastStartTime())
                .setLastStopTime(params.getLastStopTime())
                .setProcessId(params.getProcessId())
                .setRunning(params.getRunning());
        DeploymentRecord saved = deploymentRecordRepository.save(deployment);
        return deploymentRecordPoVoMapper.poToVo(saved);
    }


    /**
     * 查询部署记录列表
     *
     * @param params 查询参数
     * @param sort   排序参数
     * @return 部署记录列表
     */
    public List<DeploymentRecordVo> queryList(DeploymentParams params, Sort sort) {
        Specification<DeploymentRecord> specification = buildSpecification(params);
        return deploymentRecordPoVoMapper.poListToVoList(deploymentRecordRepository.findAll(specification, sort));
    }

    /**
     * 分页查询部署记录列表
     *
     * @param params   查询参数
     * @param pageable 分页参数
     * @return 分页结果
     */
    public PageResult<DeploymentRecordVo> queryPage(DeploymentParams params, Pageable pageable) {
        Specification<DeploymentRecord> specification = buildSpecification(params);
        Page<DeploymentRecord> page = deploymentRecordRepository.findAll(specification, pageable);
        List<DeploymentRecordVo> deploymentList = deploymentRecordPoVoMapper.poListToVoList(page.getContent());
        return new PageResult<>(deploymentList, pageable, page.getTotalElements());
    }

    /**
     * 查询部署记录
     *
     * @param id 部署记录 Id
     * @return 部署记录信息
     */
    public DeploymentRecordVo queryDeployment(String id) {
        DeploymentRecord deployment = getDeployment(id);
        return deploymentRecordPoVoMapper.poToVo(deployment);
    }

    /**
     * 更新部署记录
     *
     * @param id     部署记录 Id
     * @param params 部署参数
     * @return 更新后的部署记录信息
     */
    @Transactional
    public DeploymentRecordVo updateDeployment(String id, DeploymentParams params) {
        DeploymentRecord deployment = getDeployment(id);
        ServerRecord serverRecord = serverService.getServer(params.getServerRecordId());
        FileRecord fileRecord = fileStorageService.getFileRecord(params.getFileRecordId());

        deployment.setServerRecord(serverRecord)
                .setFileRecord(fileRecord)
                .setApplicationType(params.getApplicationType())
                .setDeploymentPath(params.getDeploymentPath())
                .setDeploymentConfigPath(params.getDeploymentConfigPath())
                .setPort(params.getPort())
                .setProgramArgs(params.getProgramArgs())
                .setActiveProfiles(params.getActiveProfiles())
                .setStatus(params.getStatus())
                .setErrorMessage(params.getErrorMessage())
                .setLastStartTime(params.getLastStartTime())
                .setLastStopTime(params.getLastStopTime())
                .setProcessId(params.getProcessId())
                .setRunning(params.getRunning());
        DeploymentRecord updated = deploymentRecordRepository.save(deployment);
        return deploymentRecordPoVoMapper.poToVo(updated);
    }

    /**
     * 删除部署记录
     *
     * @param id 部署记录 Id
     */
    @Transactional
    public void deleteDeployment(String id) {
        DeploymentRecord deployment = getDeployment(id);
        deploymentRecordRepository.delete(deployment);
    }

    /**
     * 获取部署记录 PO
     *
     * @param id 部署记录 Id
     * @return 部署记录 PO
     */
    private DeploymentRecord getDeployment(String id) {
        return deploymentRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("部署记录 [%s] 不存在", id)));
    }

    /**
     * 构建复杂查询参数
     *
     * @param params 查询参数
     * @return 复杂查询参数
     */
    private Specification<DeploymentRecord> buildSpecification(DeploymentParams params) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(params.getServerRecordId())) {
                predicates.add(criteriaBuilder.equal(root.get("serverRecord").get("id"), params.getServerRecordId()));
            }

            if (StringUtils.hasText(params.getFileRecordId())) {
                predicates.add(criteriaBuilder.equal(root.get("fileRecord").get("id"), params.getFileRecordId()));
            }

            if (params.getApplicationType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("applicationType"), params.getApplicationType()));
            }

            if (StringUtils.hasText(params.getDeploymentPath())) {
                predicates.add(criteriaBuilder.like(root.get("deploymentPath"), "%" + params.getDeploymentPath() + "%"));
            }

            if (params.getPort() != null) {
                predicates.add(criteriaBuilder.equal(root.get("port"), params.getPort()));
            }

            if (StringUtils.hasText(params.getActiveProfiles())) {
                predicates.add(criteriaBuilder.like(root.get("activeProfiles"), "%" + params.getActiveProfiles() + "%"));
            }

            if (params.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), params.getStatus()));
            }

            if (params.getRunning() != null) {
                predicates.add(criteriaBuilder.equal(root.get("running"), params.getRunning()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 启动应用
     *
     * @param id 部署记录 Id
     * @return 更新后的部署记录信息
     */
    @Transactional(rollbackFor = Exception.class)
    public DeploymentRecordVo startApplication(String id) {
        DeploymentRecord deployment = getDeployment(id);

        // 检查应用类型
        if (deployment.getApplicationType() != ApplicationTypeEnum.BACKEND) {
            throw new InvalidOperationException("操作失败: 只有后端应用才能启动");
        }

        // 检查应用状态
        if (Boolean.TRUE.equals(deployment.getRunning())) {
            throw new InvalidOperationException("操作失败: 应用已经在运行中");
        }

        doStart(deployment);
        return deploymentRecordPoVoMapper.poToVo(deployment);
    }

    /**
     * 停止应用
     *
     * @param id 部署记录 Id
     * @return 更新后的部署记录信息
     */
    @Transactional(rollbackFor = Exception.class)
    public DeploymentRecordVo stopApplication(String id) {
        DeploymentRecord deployment = getDeployment(id);

        // 检查应用类型
        if (deployment.getApplicationType() != ApplicationTypeEnum.BACKEND) {
            throw new InvalidOperationException("操作失败：只有后端应用才能停止");
        }

        // 先获取应用状态
        DeploymentRecordVo status = getApplicationStatus(id);
        if (!Boolean.TRUE.equals(status.getRunning())) {
            // 如果应用未运行，更新记录状态
            deployment.setRunning(false)
                    .setLastStopTime(LocalDateTime.now())
                    .setProcessId(null);
            DeploymentRecord updated = deploymentRecordRepository.save(deployment);
            return deploymentRecordPoVoMapper.poToVo(updated);
        }

        doStop(deployment);
        return deploymentRecordPoVoMapper.poToVo(deployment);
    }

    /**
     * 重启应用
     *
     * <p>所有改动都包在同一个事务里：若 doStart 中途失败，doStop 已对实体做的状态变更会随事务一起回滚，
     * 不再出现"已停止状态被提交、启动失败后无法回退"的不一致状态。
     *
     * @param id 部署记录 Id
     * @return 更新后的部署记录信息
     */
    @Transactional(rollbackFor = Exception.class)
    public DeploymentRecordVo restartApplication(String id) {
        DeploymentRecord deployment = getDeployment(id);

        if (deployment.getApplicationType() != ApplicationTypeEnum.BACKEND) {
            throw new InvalidOperationException("操作失败: 只有后端应用才能重启");
        }

        if (Boolean.TRUE.equals(deployment.getRunning())) {
            doStop(deployment);
        }
        doStart(deployment);
        return deploymentRecordPoVoMapper.poToVo(deployment);
    }

    /**
     * 在调用方事务内执行启动逻辑（不再做应用类型与运行状态校验，由调用方保证前置条件）。
     */
    private void doStart(DeploymentRecord deployment) {
        try {
            ServerRecordDto server = serverService.getServerDto(deployment.getServerRecord().getId());

            String command = String.format(
                    "cd %s; " +
                            "nohup java -jar %s --server.port=%d %s --spring.profiles.active=%s > nohup.out 2>&1 & " +
                            "PID=$!; " +
                            "echo $PID; " +
                            "disown $PID; " +
                            "exit 0",
                    deployment.getDeploymentPath(),
                    deployment.getFileRecord().getFileName(),
                    deployment.getPort(),
                    deployment.getProgramArgs(),
                    deployment.getActiveProfiles()
            );

            String result = sshService.executeCommand(server, command);
            String processId = result.trim();

            deployment.setStatus(DeploymentStatusEnum.SUCCESS)
                    .setRunning(true)
                    .setProcessId(processId)
                    .setLastStartTime(LocalDateTime.now())
                    .setErrorMessage(null);
            deploymentRecordRepository.save(deployment);
        } catch (BusinessException | InvalidOperationException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(String.format("启动应用失败: %s", e.getMessage()), e);
        }
    }

    /**
     * 在调用方事务内执行停止逻辑（不再做应用类型与运行状态校验，由调用方保证前置条件）。
     */
    private void doStop(DeploymentRecord deployment) {
        try {
            ServerRecordDto server = serverService.getServerDto(deployment.getServerRecord().getId());

            String command = String.format("kill -15 %s", deployment.getProcessId());
            sshService.executeCommand(server, command);

            deployment.setRunning(false)
                    .setLastStopTime(LocalDateTime.now())
                    .setProcessId(null);
            deploymentRecordRepository.save(deployment);
        } catch (BusinessException | InvalidOperationException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(String.format("停止应用失败: %s", e.getMessage()), e);
        }
    }

    /**
     * 获取应用状态
     *
     * @param id 部署记录 Id
     * @return 更新后的部署记录信息
     */
    public DeploymentRecordVo getApplicationStatus(String id) {
        DeploymentRecord deployment = getDeployment(id);

        // 检查应用类型
        if (deployment.getApplicationType() != ApplicationTypeEnum.BACKEND) {
            throw new BusinessException("只有后端应用才能查询状态");
        }

        // 检查应用是否在运行
        if (!Boolean.TRUE.equals(deployment.getRunning())) {
            return deploymentRecordPoVoMapper.poToVo(deployment);
        }

        try {
            // 获取服务器信息
            ServerRecordDto server = serverService.getServerDto(deployment.getServerRecord().getId());

            // 构建检查命令
            String command = String.format("ps -p %s > /dev/null && echo 'running' || echo 'stopped'",
                    deployment.getProcessId());

            // 执行检查命令
            String result = sshService.executeCommand(server, command);

            // 更新运行状态
            boolean isRunning = "running".equals(result.trim());
            if (isRunning != deployment.getRunning()) {
                deployment.setRunning(isRunning)
                        .setLastStopTime(isRunning ? null : LocalDateTime.now());
                deployment = deploymentRecordRepository.save(deployment);
            }

            return deploymentRecordPoVoMapper.poToVo(deployment);
        } catch (Exception e) {
            throw new BusinessException(String.format("获取应用状态失败: %s", e.getMessage()), e);
        }
    }

    /**
     * 更新应用
     *
     * @param id           部署记录 Id
     * @param fileRecordId 新的文件记录 Id
     * @return 更新后的部署记录信息
     */
    @Transactional
    public DeploymentRecordVo updateApplication(String id, String fileRecordId) {
        DeploymentRecord deployment = getDeployment(id);
        FileRecord newFileRecord = fileStorageService.getFileRecord(fileRecordId);

        // 检查文件类型是否匹配
        if (deployment.getApplicationType() == ApplicationTypeEnum.BACKEND && !newFileRecord.getFileName().endsWith(".jar")) {
            throw new InvalidOperationException("更新失败: 后端应用只能更新 jar 包");
        }
        if (deployment.getApplicationType() == ApplicationTypeEnum.FRONTEND && !newFileRecord.getFileName().endsWith(".zip")) {
            throw new InvalidOperationException("更新失败: 前端应用只能更新 zip 包");
        }

        try {
            if (deployment.getApplicationType() == ApplicationTypeEnum.BACKEND) {
                // 如果是后端应用，重启应用
                // 更新部署记录的文件记录
                deployment.setFileRecord(newFileRecord);
                deploymentRecordRepository.save(deployment);
                return restartApplication(id);
            } else {
                // 如果是前端应用，解压压缩包
                // 获取服务器信息
                ServerRecordDto server = serverService.getServerDto(deployment.getServerRecord().getId());
                // 建立 SSH 连接
                String sessionId = sshService.connect(server);
                try {
                    // 解压文件
                    String unzipCommand = String.format(
                            "cd %s && unzip -o %s",
                            deployment.getDeploymentPath(),
                            newFileRecord.getFileName()
                    );
                    sshService.executeCommand(server, unzipCommand);

                    // 更新部署记录
                    deployment.setFileRecord(newFileRecord)
                            .setStatus(DeploymentStatusEnum.SUCCESS)
                            .setErrorMessage(null);
                    DeploymentRecord updated = deploymentRecordRepository.save(deployment);
                    return deploymentRecordPoVoMapper.poToVo(updated);
                } finally {
                    // 确保断开连接
                    sshService.disconnect(sessionId);
                }
            }
        } catch (Exception e) {
            throw new BusinessException(String.format("更新应用失败: %s", e.getMessage()), e);
        }
    }

}