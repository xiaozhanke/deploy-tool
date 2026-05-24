package com.xiaozhanke.deploy.repository;

import com.xiaozhanke.deploy.model.entity.DeploymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * 部署记录持久化接口
 *
 * @author xiaozhanke
 */
@Repository
public interface DeploymentRecordRepository extends JpaRepository<DeploymentRecord, String>, JpaSpecificationExecutor<DeploymentRecord> {
}