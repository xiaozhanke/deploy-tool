package com.xiaozhanke.deploy.repository;

import com.xiaozhanke.deploy.model.entity.PlatformUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * 用户持久化接口
 *
 * @author xiaozhanke
 */
@Repository
public interface PlatformUserRepository extends JpaRepository<PlatformUser, String>, JpaSpecificationExecutor<PlatformUser> {
    Optional<PlatformUser> findByUsername(String username);
}
