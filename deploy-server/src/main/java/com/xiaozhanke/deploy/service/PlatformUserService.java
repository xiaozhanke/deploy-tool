package com.xiaozhanke.deploy.service;

import com.xiaozhanke.deploy.exception.DuplicateResourceException;
import com.xiaozhanke.deploy.exception.InvalidOperationException;
import com.xiaozhanke.deploy.exception.ResourceNotFoundException;
import com.xiaozhanke.deploy.model.entity.PlatformRole;
import com.xiaozhanke.deploy.model.entity.PlatformUser;
import com.xiaozhanke.deploy.model.mapper.PlatformUserPoVoMapper;
import com.xiaozhanke.deploy.model.request.UserParams;
import com.xiaozhanke.deploy.model.request.UserPasswordParams;
import com.xiaozhanke.deploy.model.request.UserProfileParams;
import com.xiaozhanke.deploy.model.response.PageResult;
import com.xiaozhanke.deploy.model.vo.PlatformUserVo;
import com.xiaozhanke.deploy.repository.PlatformRoleRepository;
import com.xiaozhanke.deploy.repository.PlatformUserRepository;
import com.xiaozhanke.deploy.util.AuthenticationHelper;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户服务类
 *
 * @author xiaozhanke
 */
@Slf4j
@Service
public class PlatformUserService {

    private final PlatformUserRepository userRepository;
    private final PlatformRoleRepository roleRepository;
    private final PlatformUserPoVoMapper userPoVoMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationHelper authenticationHelper;

    public PlatformUserService(PlatformUserRepository userRepository, PlatformRoleRepository roleRepository,
                               PlatformUserPoVoMapper userPoVoMapper, PasswordEncoder passwordEncoder,
                               AuthenticationHelper authenticationHelper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userPoVoMapper = userPoVoMapper;
        this.passwordEncoder = passwordEncoder;
        this.authenticationHelper = authenticationHelper;
    }

    /**
     * 创建用户
     *
     * @param params 用户参数
     * @return 保存的用户信息
     */
    @Transactional
    public PlatformUserVo createUser(UserParams params) {
        String username = params.getUsername();
        userRepository.findByUsername(username).ifPresent(user -> {
            throw new DuplicateResourceException(String.format("用户名 [%s] 已存在", username));
        });
        PlatformUser user = new PlatformUser();
        BeanUtils.copyProperties(params, user);
        user.setPassword(passwordEncoder.encode(params.getPassword()));
        validateAndAssignRoles(user, params);
        PlatformUser saved = userRepository.save(user);
        return userPoVoMapper.poToVo(saved);
    }

    /**
     * 分页查询用户列表
     *
     * @param params   查询参数
     * @param pageable 分页参数
     * @return 分页结果
     */
    public PageResult<PlatformUserVo> queryPage(PlatformUserVo params, Pageable pageable) {
        Specification<PlatformUser> specification = buildSpecification(params);
        Page<PlatformUser> page = userRepository.findAll(specification, pageable);
        List<PlatformUserVo> userList = userPoVoMapper.poListToVoList(page.getContent());
        return new PageResult<>(userList, pageable, page.getTotalElements());
    }

    /**
     * 查询用户
     *
     * @param id 用户 Id
     * @return 用户信息
     */
    public PlatformUserVo queryUser(String id) {
        PlatformUser user = getUser(id);
        return userPoVoMapper.poToVo(user);
    }

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    public PlatformUserVo queryUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(userPoVoMapper::poToVo)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("用户 [%s] 不存在", username)));
    }

    /**
     * 更新用户
     *
     * @param id     用户 Id
     * @param params 用户参数
     * @return 更新后的用户信息
     */
    @Transactional
    public PlatformUserVo updateUser(String id, UserParams params) {
        PlatformUser user = getUser(id);
        String username = params.getUsername();
        if (!user.getUsername().equals(username)) {
            userRepository.findByUsername(username).ifPresent(exist -> {
                throw new DuplicateResourceException(String.format("用户名 [%s] 已存在", username));
            });
        }
        String password = user.getPassword();
        BeanUtils.copyProperties(params, user);
        user.setPassword(password);
        validateAndAssignRoles(user, params);
        PlatformUser updated = userRepository.save(user);
        return userPoVoMapper.poToVo(updated);
    }

    /**
     * 删除用户
     *
     * @param id 用户 Id
     */
    @Transactional
    public void deleteUser(String id) {
        PlatformUser user = getUser(id);
        // 清除角色关联
        user.getRoles().clear();
        userRepository.save(user);
        userRepository.delete(user);
    }

    /**
     * 获取用户 PO
     *
     * @param id 用户 Id
     * @return 用户 PO
     */
    private PlatformUser getUser(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("用户 [%s] 不存在", id)));
    }

    /**
     * 角色验证并给用户分配角色
     *
     * @param user   用户
     * @param params 用户参数
     */
    private void validateAndAssignRoles(PlatformUser user, UserParams params) {
        List<PlatformRole> roles = roleRepository.findByNameIn(params.getRoleNames());
        if (roles.size() != params.getRoleNames().size()) {
            Set<String> foundRoleNames = roles.stream().map(PlatformRole::getName).collect(Collectors.toSet());
            Set<String> missingRoleNames = new HashSet<>(params.getRoleNames());
            missingRoleNames.removeAll(foundRoleNames);
            throw new ResourceNotFoundException(String.format("未找到角色 [%s]", missingRoleNames));
        }
        user.setRoles(roles);
    }

    /**
     * 构建复杂查询参数
     *
     * @param params 查询参数
     * @return 复杂查询参数
     */
    private Specification<PlatformUser> buildSpecification(PlatformUserVo params) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicateList = new ArrayList<>();

            if (StringUtils.hasText(params.getId())) {
                predicateList.add(criteriaBuilder.equal(root.get("id"), params.getId()));
            }

            if (StringUtils.hasText(params.getUsername())) {
                predicateList.add(criteriaBuilder.like(root.get("username"), "%" + params.getUsername() + "%"));
            }

            if (StringUtils.hasText(params.getDisplayName())) {
                predicateList.add(criteriaBuilder.like(root.get("displayName"), "%" + params.getDisplayName() + "%"));
            }

            if (params.getStatus() != null) {
                predicateList.add(criteriaBuilder.equal(root.get("status"), params.getStatus()));
            }

            return criteriaBuilder.and(predicateList.toArray(new Predicate[0]));
        };
    }

    /**
     * 更新当前用户信息
     *
     * @param params 用户信息参数
     * @return 保存后的用户信息
     */
    @Transactional
    public PlatformUserVo updateCurrentUserProfile(UserProfileParams params) {
        String currentUserName = authenticationHelper.getCurrentUserName()
                .orElseThrow(() -> new IllegalStateException("用户未认证"));
        PlatformUser user = userRepository.findByUsername(currentUserName)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("当前用户 [%s] 不存在", currentUserName)));
        String username = params.getUsername();
        if (!user.getUsername().equals(username)) {
            userRepository.findByUsername(username).ifPresent(exist -> {
                throw new DuplicateResourceException(String.format("用户名 [%s] 已存在", username));
            });
        }
        BeanUtils.copyProperties(params, user);
        PlatformUser updated = userRepository.save(user);
        return userPoVoMapper.poToVo(updated);
    }

    /**
     * 更新当前用户密码
     *
     * @param params 修改密码参数
     * @return 保存后的用户信息
     */
    @Transactional
    public PlatformUserVo updateCurrentUserPassword(UserPasswordParams params) {
        String currentUserName = authenticationHelper.getCurrentUserName()
                .orElseThrow(() -> new IllegalStateException("用户未认证"));

        // 获取当前用户
        PlatformUser user = userRepository.findByUsername(currentUserName)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("当前用户 [%s] 不存在", currentUserName)));

        // 验证旧密码
        if (!passwordEncoder.matches(params.getOldPassword(), user.getPassword())) {
            throw new InvalidOperationException("旧密码错误");
        }

        // 确保新密码和确认密码一致
        if (!params.getNewPassword().equals(params.getConfirmPassword())) {
            throw new InvalidOperationException("新密码与确认密码不匹配");
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(params.getNewPassword()));
        user.setPasswordLastChangedTime(LocalDateTime.now());

        PlatformUser updated = userRepository.save(user);
        return userPoVoMapper.poToVo(updated);
    }
}
