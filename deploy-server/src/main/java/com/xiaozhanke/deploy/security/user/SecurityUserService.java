package com.xiaozhanke.deploy.security.user;

import com.xiaozhanke.deploy.enums.UserStatusEnum;
import com.xiaozhanke.deploy.model.entity.PlatformUser;
import com.xiaozhanke.deploy.repository.PlatformUserRepository;
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证用户服务类
 *
 * @author xiaozhanke
 */
@Slf4j
@Service
public class SecurityUserService implements UserDetailsService {

    /**
     * 密码有效期（天数）。
     *
     * <p>{@code &gt; 0}：密码自最后修改时间起 N 天内有效，过期后由 Spring Security 拒绝登录。<br>
     * {@code &lt;= 0}：禁用密码有效期约束（永不过期），仅在内网/低敏感环境使用。
     */
    @Value("${app.security.password-validity-days:365}")
    private int passwordValidityDays;

    /**
     * 允许连续登录失败的最大次数。
     *
     * <p>{@code &gt; 0}：连续失败达到该值即锁定账户，需后台解锁。<br>
     * {@code &lt;= 0}：禁用登录失败锁定（仅暴力破解防御层关闭，{@link UserStatusEnum#LOCKED} 仍然生效）。
     */
    @Value("${app.security.max-failed-logins:5}")
    private int maxFailedLogins;

    private final PlatformUserRepository platformUserRepository;

    public SecurityUserService(PlatformUserRepository platformUserRepository) {
        this.platformUserRepository = platformUserRepository;
    }

    /**
     * 启动后输出当前生效的安全策略，避免 0 / 负值这种"静默放行"配置无人察觉。
     */
    @PostConstruct
    void logSecurityPolicy() {
        if (passwordValidityDays <= 0) {
            log.warn("密码有效期已禁用（app.security.password-validity-days={}），密码将永不过期", passwordValidityDays);
        } else {
            log.info("密码有效期 {} 天", passwordValidityDays);
        }
        if (maxFailedLogins <= 0) {
            log.warn("登录失败锁定已禁用（app.security.max-failed-logins={}），账户不会因连续失败被自动锁定",
                    maxFailedLogins);
        } else {
            log.info("登录失败锁定阈值 {} 次", maxFailedLogins);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 必须用 findWithRolesByUsername：roles 改 LAZY 后，open-in-view=false 下脱离事务访问会抛 LazyInitializationException
        PlatformUser user = platformUserRepository.findWithRolesByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(String.format("用户 [%s] 未找到", username)));

        Set<GrantedAuthority> authorities = user.getRoles().stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName())).collect(Collectors.toSet());

        // 用户启用状态
        boolean enabled = user.getStatus() == UserStatusEnum.ACTIVE || user.getStatus() == UserStatusEnum.INITIALIZED;
        // 用户未过期状态
        boolean accountNonExpired = true;
        if (user.getAccountExpiredTime() != null) {
            accountNonExpired = user.getAccountExpiredTime().isAfter(LocalDateTime.now());
        }
        // 用户未锁定状态：LOCKED 永远代表手工锁定；maxFailedLogins <= 0 表示禁用失败锁定
        boolean accountNonLocked = isAccountNonLocked(user.getStatus(), user.getFailedLoginCount());
        // 密码未过期状态：passwordValidityDays <= 0 视为永不过期
        boolean credentialsNonExpired = isCredentialsNonExpired(user.getPasswordLastChangedTime());

        return new SecurityUser(user.getUsername(), user.getDisplayName(), user.getPassword(), user.getPhone(), user.getEmail(), authorities, enabled, accountNonExpired, accountNonLocked, credentialsNonExpired);
    }

    /**
     * 判断账户是否未被失败锁定。
     *
     * @param status            账户状态
     * @param failedLoginCount  当前连续失败次数
     * @return true 表示账户未被锁定
     */
    boolean isAccountNonLocked(UserStatusEnum status, int failedLoginCount) {
        if (status == UserStatusEnum.LOCKED) {
            return false;
        }
        if (maxFailedLogins <= 0) {
            return true;
        }
        return failedLoginCount < maxFailedLogins;
    }

    /**
     * 判断密码是否在有效期内。
     *
     * @param passwordLastChangedTime 密码上次修改时间，可能为 null（如初始化用户）
     * @return true 表示凭证未过期
     */
    boolean isCredentialsNonExpired(LocalDateTime passwordLastChangedTime) {
        if (passwordValidityDays <= 0 || passwordLastChangedTime == null) {
            return true;
        }
        return passwordLastChangedTime.plusDays(passwordValidityDays).isAfter(LocalDateTime.now());
    }

    /**
     * 测试入口：注入有效期天数。
     */
    void setPasswordValidityDays(int passwordValidityDays) {
        this.passwordValidityDays = passwordValidityDays;
    }

    /**
     * 测试入口：注入失败锁定阈值。
     */
    void setMaxFailedLogins(int maxFailedLogins) {
        this.maxFailedLogins = maxFailedLogins;
    }
}
