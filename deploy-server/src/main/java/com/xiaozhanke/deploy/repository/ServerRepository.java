package com.xiaozhanke.deploy.repository;

import com.xiaozhanke.deploy.model.entity.ServerRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 服务器信息持久化接口
 *
 * @author xiaozhanke
 */
@Repository
public interface ServerRepository extends JpaRepository<ServerRecord, String>, JpaSpecificationExecutor<ServerRecord> {
    Optional<ServerRecord> findByIdAndDeletedIsFalse(String id);

    List<ServerRecord> findAllByDeletedIsFalse();
}