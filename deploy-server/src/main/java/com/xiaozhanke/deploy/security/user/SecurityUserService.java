package com.xiaozhanke.deploy.security.user;

import com.xiaozhanke.deploy.enums.UserStatusEnum;
import com.xiaozhanke.deploy.model.entity.PlatformUser;
import com.xiaozhanke.deploy.repository.PlatformUserRepository;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;
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
@Service
public class SecurityUserService implements UserDetailsService {

    /**
     * 密码有效期（天数）
     */
    @Value("${app.security.password-validity-days:365}")
    private int passwordValidityDays;

    /**
     * 允许连续登录失败的最大次数
     */
    @Value("${app.security.max-failed-logins:5}")
    private int maxFailedLogins;

    private final PlatformUserRepository platformUserRepository;

    public SecurityUserService(PlatformUserRepository platformUserRepository) {
        this.platformUserRepository = platformUserRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        PlatformUser user = platformUserRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException(String.format("用户 [%s] 未找到", username)));

        Set<GrantedAuthority> authorities = user.getRoles().stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName())).collect(Collectors.toSet());

        // 用户启用状态
        boolean enabled = user.getStatus() == UserStatusEnum.ACTIVE || user.getStatus() == UserStatusEnum.INITIALIZED;
        // 用户未过期状态
        boolean accountNonExpired = true;
        if (user.getAccountExpiredTime() != null) {
            accountNonExpired = user.getAccountExpiredTime().isAfter(LocalDateTime.now());
        }
        // 用户未锁定状态
        boolean accountNonLocked = user.getStatus() != UserStatusEnum.LOCKED;
        if (user.getFailedLoginCount() >= maxFailedLogins) {
            accountNonLocked = false;
        }
        // 密码未过期状态
        boolean credentialsNonExpired = true;
        if (user.getPasswordLastChangedTime() != null && passwordValidityDays > 0) {
            LocalDateTime passwordExpiryDate = user.getPasswordLastChangedTime().plusDays(passwordValidityDays);
            credentialsNonExpired = passwordExpiryDate.isAfter(LocalDateTime.now());
        }

        return new SecurityUser(user.getUsername(), user.getDisplayName(), user.getPassword(), user.getPhone(), user.getEmail(), authorities, enabled, accountNonExpired, accountNonLocked, credentialsNonExpired);
    }
}
