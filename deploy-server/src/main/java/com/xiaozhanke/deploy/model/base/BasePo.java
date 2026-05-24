package com.xiaozhanke.deploy.model.base;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 基础 PO 类
 *
 * @author xiaozhanke
 */
@Data
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class BasePo {
    /**
     * 创建时间
     */
    @Comment("创建时间")
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createTime;

    /**
     * 创建用户
     */
    @Comment("创建用户")
    @CreatedBy
    @Column(nullable = false, updatable = false)
    private String createUser;

    /**
     * 更新时间
     */
    @Comment("更新时间")
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updateTime;

    /**
     * 更新用户
     */
    @Comment("更新用户")
    @LastModifiedBy
    @Column(nullable = false)
    private String updateUser;

    /**
     * 已删除
     */
    @Comment("已删除")
    @Column(name = "is_deleted")
    private Boolean deleted = Boolean.FALSE;
}
