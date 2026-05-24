package com.xiaozhanke.deploy.service;

import com.xiaozhanke.deploy.exception.DuplicateResourceException;
import com.xiaozhanke.deploy.exception.ResourceNotFoundException;
import com.xiaozhanke.deploy.model.entity.PlatformRole;
import com.xiaozhanke.deploy.model.mapper.PlatformRolePoVoMapper;
import com.xiaozhanke.deploy.model.request.RoleParams;
import com.xiaozhanke.deploy.model.response.PageResult;
import com.xiaozhanke.deploy.model.vo.PlatformRoleVo;
import com.xiaozhanke.deploy.repository.PlatformRoleRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 角色服务类
 *
 * @author xiaozhanke
 */
@Slf4j
@Service
public class PlatformRoleService {

    private final PlatformRoleRepository roleRepository;
    private final PlatformRolePoVoMapper rolePoVoMapper;

    public PlatformRoleService(PlatformRoleRepository roleRepository, PlatformRolePoVoMapper rolePoVoMapper) {
        this.roleRepository = roleRepository;
        this.rolePoVoMapper = rolePoVoMapper;
    }

    /**
     * 创建角色
     *
     * @param params 角色参数
     * @return 保存后的角色信息
     */
    @Transactional
    public PlatformRoleVo createRole(RoleParams params) {
        String roleName = params.getName();
        roleRepository.findByName(roleName).ifPresent(user -> {
            throw new DuplicateResourceException(String.format("角色名 [%s] 已存在", roleName));
        });
        PlatformRole role = new PlatformRole();
        BeanUtils.copyProperties(params, role);
        PlatformRole saved = roleRepository.save(role);
        return rolePoVoMapper.poToVo(saved);
    }

    /**
     * 分页查询角色列表
     *
     * @param params   查询参数
     * @param pageable 分页参数
     * @return 分页结果
     */
    public PageResult<PlatformRoleVo> queryPage(PlatformRoleVo params, Pageable pageable) {
        Specification<PlatformRole> specification = buildSpecification(params);
        Page<PlatformRole> page = roleRepository.findAll(specification, pageable);
        List<PlatformRoleVo> roleList = rolePoVoMapper.poListToVoList(page.getContent());
        return new PageResult<>(roleList, pageable, page.getTotalElements());
    }

    /**
     * 查询角色
     *
     * @param id 角色 Id
     * @return 角色信息
     */
    public PlatformRoleVo queryRole(String id) {
        PlatformRole role = getRole(id);
        return rolePoVoMapper.poToVo(role);
    }

    /**
     * 更新角色
     *
     * @param id     角色 Id
     * @param params 角色参数
     * @return 更新后的角色信息
     */
    @Transactional
    public PlatformRoleVo updateRole(String id, RoleParams params) {
        PlatformRole role = getRole(id);
        String roleName = params.getName();
        if (!role.getName().equals(roleName)) {
            roleRepository.findByName(roleName).ifPresent(exist -> {
                throw new DuplicateResourceException(String.format("角色名 [%s] 已存在", roleName));
            });
        }
        BeanUtils.copyProperties(params, role);
        PlatformRole updated = roleRepository.save(role);
        return rolePoVoMapper.poToVo(updated);
    }

    /**
     * 删除角色
     *
     * @param id 角色 Id
     */
    @Transactional
    public void deleteRole(String id) {
        PlatformRole role = getRole(id);
        // 清除用户关联
        role.getUsers().clear();
        roleRepository.save(role);
        roleRepository.delete(role);
    }

    /**
     * 获取角色 PO
     *
     * @param id 角色 Id
     * @return 角色 PO
     */
    private PlatformRole getRole(String id) {
        return roleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(String.format("角色 [%s] 不存在", id)));
    }

    /**
     * 构建复杂查询参数
     *
     * @param params 查询参数
     * @return 复杂查询参数
     */
    private Specification<PlatformRole> buildSpecification(PlatformRoleVo params) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicateList = new ArrayList<>();

            if (StringUtils.hasText(params.getId())) {
                predicateList.add(criteriaBuilder.equal(root.get("id"), params.getId()));
            }

            if (StringUtils.hasText(params.getName())) {
                predicateList.add(criteriaBuilder.like(root.get("name"), "%" + params.getName() + "%"));
            }

            if (StringUtils.hasText(params.getDescription())) {
                predicateList.add(criteriaBuilder.like(root.get("description"), "%" + params.getDescription() + "%"));
            }

            return criteriaBuilder.and(predicateList.toArray(new Predicate[0]));
        };
    }

}
