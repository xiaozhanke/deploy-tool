package com.xiaozhanke.deploy.model.mapper;

import com.xiaozhanke.deploy.model.base.BasePoDtoMapper;
import com.xiaozhanke.deploy.model.dto.DeploymentRecordDto;
import com.xiaozhanke.deploy.model.entity.DeploymentRecord;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * 部署记录 PO DTO 转换器
 *
 * @author xiaozhanke
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DeploymentRecordPoDtoMapper extends BasePoDtoMapper<DeploymentRecord, DeploymentRecordDto> {
}
