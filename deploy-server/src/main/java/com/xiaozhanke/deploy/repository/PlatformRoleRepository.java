package com.xiaozhanke.deploy.repository;

import com.xiaozhanke.deploy.model.entity.PlatformRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * 角色持久化接口
 *
 * @author xiaozhanke
 */
@Repository
public interface PlatformRoleRepository extends JpaRepository<PlatformRole, String>, JpaSpecificationExecutor<PlatformRole> {
    Optional<PlatformRole> findByName(String name);

    List<PlatformRole> findByNameIn(List<String> names);
}
