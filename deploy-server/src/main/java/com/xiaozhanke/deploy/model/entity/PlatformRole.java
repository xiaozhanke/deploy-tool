package com.xiaozhanke.deploy.model.entity;

import com.xiaozhanke.deploy.model.base.BasePo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.hibernate.annotations.Comment;

/**
 * 角色
 *
 * @author xiaozhanke
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Entity
@Table(name = "platform_role")
@Comment("角色表")
public class PlatformRole extends BasePo {

    /**
     * 角色 Id
     */
    @Comment("角色 Id")
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * 角色名
     */
    @Comment("角色名")
    @Column(unique = true, nullable = false)
    private String name;

    /**
     * 角色描述
     */
    @Comment("角色描述")
    @Column
    private String description;

    @ManyToMany(mappedBy = "roles")
    private List<PlatformUser> users = new ArrayList<>();
}
