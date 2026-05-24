package com.xiaozhanke.deploy.model.mapper;

import com.xiaozhanke.deploy.model.base.BasePoVoMapper;
import com.xiaozhanke.deploy.model.entity.PlatformRole;
import com.xiaozhanke.deploy.model.vo.PlatformRoleVo;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * 角色 PO VO 转换器
 *
 * @author xiaozhanke
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PlatformRolePoVoMapper extends BasePoVoMapper<PlatformRole, PlatformRoleVo> {
}
