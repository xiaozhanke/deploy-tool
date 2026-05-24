package com.xiaozhanke.deploy.repository;

import com.xiaozhanke.deploy.model.entity.PlatformUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * 用户持久化接口
 *
 * <p>{@link PlatformUser#getRoles()} 已改为 {@link jakarta.persistence.FetchType#LAZY}，
 * 这里通过 {@link EntityGraph} 暴露 "带 roles" 的查询方法，避免登录 / 详情场景退化为 N+1；
 * 列表分页则在 Service 层的 Specification 内显式 {@code root.fetch("roles", LEFT)} 联合加载。
 *
 * @author xiaozhanke
 */
@Repository
public interface PlatformUserRepository extends JpaRepository<PlatformUser, String>, JpaSpecificationExecutor<PlatformUser> {

    Optional<PlatformUser> findByUsername(String username);

    @EntityGraph(attributePaths = "roles")
    Optional<PlatformUser> findWithRolesByUsername(String username);

    @EntityGraph(attributePaths = "roles")
    Optional<PlatformUser> findWithRolesById(String id);
}
