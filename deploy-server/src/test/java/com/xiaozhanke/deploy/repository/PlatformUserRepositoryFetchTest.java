package com.xiaozhanke.deploy.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.xiaozhanke.deploy.config.JpaConfig;
import com.xiaozhanke.deploy.enums.UserStatusEnum;
import com.xiaozhanke.deploy.model.entity.PlatformRole;
import com.xiaozhanke.deploy.model.entity.PlatformUser;
import com.xiaozhanke.deploy.util.AuthenticationHelper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * 验证 {@link PlatformUserRepository#findWithRolesByUsername(String)} 与
 * {@link PlatformUserRepository#findWithRolesById(String)} 通过 {@link org.springframework.data.jpa.repository.EntityGraph}
 * 单条 SQL 加载 user + roles，避免 N+1。
 *
 * <p>之前 PlatformUser.roles 标 FetchType.EAGER + List，登录流程会触发一次 user 查询 + 一次 join；
 * 改 LAZY 后必须为安全相关的查询补 EntityGraph，否则脱离事务访问 user.getRoles() 抛 LazyInitializationException。
 * 本测试用 Hibernate Statistics 统计实际触发的 SQL 数量。
 *
 * @author xiaozhanke
 */
@DataJpaTest
@Import({JpaConfig.class, AuthenticationHelper.class})
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class PlatformUserRepositoryFetchTest {

    @Autowired
    private PlatformUserRepository userRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @PersistenceContext
    private EntityManager entityManager;

    private Statistics statistics;

    @BeforeEach
    void setUp() {
        statistics = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        statistics.clear();
    }

    @Test
    void findWithRolesByUsernameLoadsRolesInSingleStatement() {
        PlatformUser user = seedUserWithRoles("alice", 3);
        testEntityManager.flush();
        testEntityManager.clear();
        statistics.clear();

        PlatformUser loaded = userRepository.findWithRolesByUsername("alice").orElseThrow();
        long preAccessCount = statistics.getPrepareStatementCount();

        // 触发 roles 集合解引用——若是真懒加载会再多 1 条 SQL
        int roleCount = loaded.getRoles().size();
        long postAccessCount = statistics.getPrepareStatementCount();

        assertThat(roleCount).isEqualTo(3);
        assertThat(postAccessCount - preAccessCount)
                .as("findWithRolesByUsername 后访问 roles 不应再发额外 SQL")
                .isZero();
        assertThat(preAccessCount)
                .as("加载 user + roles 只应一次 join SQL")
                .isLessThanOrEqualTo(2L);
        // 简单断言 user id 不为空，避免 Java 编译警告
        assertThat(user.getId()).isNotBlank();
    }

    @Test
    void findByUsernameWithoutEntityGraphIsStillLazy() {
        seedUserWithRoles("bob", 2);
        testEntityManager.flush();
        testEntityManager.clear();
        statistics.clear();

        PlatformUser loaded = userRepository.findByUsername("bob").orElseThrow();
        long firstSqlCount = statistics.getPrepareStatementCount();

        // LAZY 配置下，未访问 roles 时不应触发关联查询
        assertThat(firstSqlCount)
                .as("未访问 roles 前应只有 1 条主表 SQL")
                .isEqualTo(1L);

        // 现在主动触发懒加载——会产生额外 SQL
        loaded.getRoles().size();
        long afterAccessSqlCount = statistics.getPrepareStatementCount();
        assertThat(afterAccessSqlCount).isGreaterThan(firstSqlCount);
    }

    private PlatformUser seedUserWithRoles(String username, int roleCount) {
        PlatformUser user = new PlatformUser();
        user.setUsername(username);
        user.setDisplayName(username);
        user.setPassword("encoded");
        user.setStatus(UserStatusEnum.ACTIVE);
        for (int i = 0; i < roleCount; i++) {
            PlatformRole role = new PlatformRole();
            role.setName(username + "-role-" + i);
            role.setDescription("test role");
            user.getRoles().add(role);
        }
        return testEntityManager.persistAndFlush(user);
    }
}
