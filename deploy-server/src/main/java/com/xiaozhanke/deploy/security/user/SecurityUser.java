package com.xiaozhanke.deploy.security.user;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.SpringSecurityCoreVersion;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.Assert;

/**
 * 认证用户
 *
 * @author xiaozhanke
 */
@Data
@Accessors(chain = true)
@Slf4j
public class SecurityUser implements UserDetails, CredentialsContainer {

    @Serial
    private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

    /**
     * 用户名
     */
    private final String username;

    /**
     * 用户显示名
     */
    private final String displayName;

    /**
     * 密码
     */
    private String password;

    /**
     * 手机号码
     */
    private final String phone;

    /**
     * 电子邮箱
     */
    private final String email;

    /**
     * 权限集合
     */
    private final Set<GrantedAuthority> authorities;

    /**
     * 用户启用状态
     */
    private final boolean enabled;

    /**
     * 用户未过期状态
     */
    private final boolean accountNonExpired;

    /**
     * 用户未锁定状态
     */
    private final boolean accountNonLocked;

    /**
     * 密码未过期状态
     */
    private final boolean credentialsNonExpired;

    public SecurityUser(String username, String displayName, String password, String phone, String email, Collection<? extends GrantedAuthority> authorities) {
        this(username, displayName, password, phone, email, authorities, true, true, true, true);
    }

    public SecurityUser(String username, String displayName, String password, String phone, String email, Collection<? extends GrantedAuthority> authorities, boolean enabled, boolean accountNonExpired, boolean accountNonLocked, boolean credentialsNonExpired) {
        this.username = username;
        this.displayName = displayName;
        this.password = password;
        this.phone = phone;
        this.email = email;
        this.enabled = enabled;
        this.accountNonExpired = accountNonExpired;
        this.accountNonLocked = accountNonLocked;
        this.credentialsNonExpired = credentialsNonExpired;
        this.authorities = Collections.unmodifiableSet(sortAuthorities(authorities));
    }

    @Override
    public void eraseCredentials() {
        this.password = null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return this.accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return this.credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    private static SortedSet<GrantedAuthority> sortAuthorities(Collection<? extends GrantedAuthority> authorities) {
        Assert.notNull(authorities, "不能传入空的 GrantedAuthority 集合");
        // 确保数组迭代顺序是可预测的（符合 UserDetails.getAuthorities() 约定，并参考 SEC-717）
        SortedSet<GrantedAuthority> sortedAuthorities = new TreeSet<>(new SecurityUser.AuthorityComparator());
        for (GrantedAuthority grantedAuthority : authorities) {
            Assert.notNull(grantedAuthority, "GrantedAuthority 列表中不能包含空元素");
            sortedAuthorities.add(grantedAuthority);
        }
        return sortedAuthorities;
    }

    private static class AuthorityComparator implements Comparator<GrantedAuthority>, Serializable {

        @Serial
        private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

        @Override
        public int compare(GrantedAuthority g1, GrantedAuthority g2) {
            // 每个条目在添加到集合之前都会经过检查，因此两者都不应该为空。
            // 如果 authority 为 null，说明它是一个自定义权限，应该排在其他权限之前。
            if (g2.getAuthority() == null) {
                return -1;
            }
            if (g1.getAuthority() == null) {
                return 1;
            }
            return g1.getAuthority().compareTo(g2.getAuthority());
        }

    }
}
