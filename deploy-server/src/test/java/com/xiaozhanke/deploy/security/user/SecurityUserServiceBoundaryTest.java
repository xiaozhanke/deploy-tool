package com.xiaozhanke.deploy.security.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.xiaozhanke.deploy.enums.UserStatusEnum;
import com.xiaozhanke.deploy.repository.PlatformUserRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 验证 SecurityUserService 在密码有效期 / 登录失败阈值取 0 / 负值时的边界行为。
 *
 * <p>之前 {@code passwordValidityDays} / {@code maxFailedLogins} 默认值合理但缺少语义文档，运维一旦把
 * 配置改成 0 或负值会出现"密码立即过期 / 任意一次失败即锁定"的静默风险。新逻辑明确：
 * <ul>
 *   <li>{@code passwordValidityDays <= 0} —— 永不过期，绕过 plusDays 计算</li>
 *   <li>{@code maxFailedLogins <= 0} —— 禁用失败锁定，{@link UserStatusEnum#LOCKED} 仍然能锁</li>
 * </ul>
 *
 * @author xiaozhanke
 */
@ExtendWith(MockitoExtension.class)
class SecurityUserServiceBoundaryTest {

    @Mock
    private PlatformUserRepository repository;

    private SecurityUserService service;

    @BeforeEach
    void initService() {
        service = new SecurityUserService(repository);
    }

    @Test
    void zeroPasswordValidityDisablesExpiry() {
        service.setPasswordValidityDays(0);
        // 即便密码是昨天改的也不应被判定为过期
        assertThat(service.isCredentialsNonExpired(LocalDateTime.now().minusDays(1))).isTrue();
        // null 同样视为未过期（初始化用户路径）
        assertThat(service.isCredentialsNonExpired(null)).isTrue();
    }

    @Test
    void negativePasswordValidityDisablesExpiry() {
        service.setPasswordValidityDays(-7);
        assertThat(service.isCredentialsNonExpired(LocalDateTime.now().minusYears(10))).isTrue();
    }

    @Test
    void positivePasswordValidityRejectsExpired() {
        service.setPasswordValidityDays(30);
        assertThat(service.isCredentialsNonExpired(LocalDateTime.now().minusDays(45))).isFalse();
        assertThat(service.isCredentialsNonExpired(LocalDateTime.now().minusDays(15))).isTrue();
    }

    @Test
    void zeroMaxFailedLoginsDisablesAutoLock() {
        service.setMaxFailedLogins(0);
        // 任何失败次数都不应触发自动锁定
        assertThat(service.isAccountNonLocked(UserStatusEnum.ACTIVE, 0)).isTrue();
        assertThat(service.isAccountNonLocked(UserStatusEnum.ACTIVE, 99)).isTrue();
    }

    @Test
    void negativeMaxFailedLoginsDisablesAutoLock() {
        service.setMaxFailedLogins(-1);
        assertThat(service.isAccountNonLocked(UserStatusEnum.ACTIVE, 5)).isTrue();
    }

    @Test
    void manualLockedStatusOverridesAutoLockSetting() {
        // 即便禁用自动锁定，LOCKED 状态依然要锁
        service.setMaxFailedLogins(0);
        assertThat(service.isAccountNonLocked(UserStatusEnum.LOCKED, 0)).isFalse();
    }

    @Test
    void positiveMaxFailedLoginsLocksAtThreshold() {
        service.setMaxFailedLogins(3);
        assertThat(service.isAccountNonLocked(UserStatusEnum.ACTIVE, 2)).isTrue();
        assertThat(service.isAccountNonLocked(UserStatusEnum.ACTIVE, 3)).isFalse();
        assertThat(service.isAccountNonLocked(UserStatusEnum.ACTIVE, 99)).isFalse();
    }
}
