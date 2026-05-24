package com.xiaozhanke.deploy.model.mapper;

import com.xiaozhanke.deploy.model.base.BasePoVoMapper;
import com.xiaozhanke.deploy.model.entity.PlatformUser;
import com.xiaozhanke.deploy.model.vo.PlatformUserVo;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * 用户 PO VO 转换器
 *
 * @author xiaozhanke
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PlatformUserPoVoMapper extends BasePoVoMapper<PlatformUser, PlatformUserVo> {
}
