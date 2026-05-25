package com.xiaozhanke.deploy.repository;

import com.xiaozhanke.deploy.model.entity.FileRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 文件记录持久化接口
 *
 * @author xiaozhanke
 */
@Repository
public interface FileRecordRepository extends JpaRepository<FileRecord, String>, JpaSpecificationExecutor<FileRecord> {
    Optional<FileRecord> findByIdAndDeletedIsFalse(String id);

    /**
     * 仅校验"未删除的同 ID"是否存在；用于 createDeployment / updateDeployment / updateApplication 关联前的轻量探活，
     * 避免直接 {@link JpaRepository#existsById(Object)} 把软删数据当成存在记录。
     */
    boolean existsByIdAndDeletedIsFalse(String id);
}
