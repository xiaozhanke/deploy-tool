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
}
