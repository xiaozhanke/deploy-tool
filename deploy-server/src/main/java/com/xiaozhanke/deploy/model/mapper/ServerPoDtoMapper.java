package com.xiaozhanke.deploy.model.mapper;

import com.xiaozhanke.deploy.model.base.BasePoDtoMapper;
import com.xiaozhanke.deploy.model.dto.ServerRecordDto;
import com.xiaozhanke.deploy.model.entity.ServerRecord;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * 服务器 PO DTO 转换器
 *
 * @author xiaozhanke
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ServerPoDtoMapper extends BasePoDtoMapper<ServerRecord, ServerRecordDto> {
}
