package com.xiaozhanke.deploy.model.entity;

import com.xiaozhanke.deploy.model.base.BasePo;
import com.xiaozhanke.deploy.enums.UserStatusEnum;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.hibernate.annotations.Comment;

/**
 * 用户
 *
 * @author xiaozhanke
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Entity
@Table(name = "platform_user")
@Comment("用户表")
public class PlatformUser extends BasePo {

    /**
     * 用户 Id
     */
    @Comment("用户 Id")
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * 用户名
     */
    @Comment("用户名")
    @Column(unique = true, nullable = false)
    private String username;

    /**
     * 用户显示名
     */
    @Comment("用户显示名")
    @Column(nullable = false)
    private String displayName;

    /**
     * 密码
     */
    @Comment("密码")
    @Column(nullable = false)
    private String password;

    /**
     * 手机号码
     */
    @Comment("手机号码")
    @Column
    private String phone;

    /**
     * 电子邮箱
     */
    @Comment("电子邮箱")
    @Column
    private String email;

    /**
     * 头像
     */
    @Comment("头像")
    @Column(columnDefinition = "CLOB")
    private String avatar;

    /**
     * 用户状态
     */
    @Comment("用户状态")
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserStatusEnum status;

    /**
     * 账户过期时间
     */
    @Comment("账户过期时间")
    @Column
    private LocalDateTime accountExpiredTime;

    /**
     * 密码最后修改时间
     */
    @Comment("密码最后修改时间")
    @Column
    private LocalDateTime passwordLastChangedTime;

    /**
     * 最后尝试登录失败时间
     */
    @Comment("最后尝试登录失败时间")
    @Column
    private LocalDateTime lastFailedLoginTime;

    /**
     * 连续登录失败次数
     */
    @Comment("连续登录失败次数")
    @Column
    private int failedLoginCount;

    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "platform_user_role",
            joinColumns = @JoinColumn(name = "platform_user_id"),
            inverseJoinColumns = @JoinColumn(name = "platform_role_id")
    )
    @Comment("用户角色关联表")
    private List<PlatformRole> roles = new ArrayList<>();

    public PlatformUser() {
        this.status = UserStatusEnum.INITIALIZED;
        this.passwordLastChangedTime = LocalDateTime.now();
        this.failedLoginCount = 0;
    }
}
