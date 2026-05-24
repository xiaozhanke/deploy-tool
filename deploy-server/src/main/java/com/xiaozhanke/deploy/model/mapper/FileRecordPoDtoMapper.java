package com.xiaozhanke.deploy.model.mapper;

import com.xiaozhanke.deploy.model.base.BasePoDtoMapper;
import com.xiaozhanke.deploy.model.dto.FileRecordDto;
import com.xiaozhanke.deploy.model.entity.FileRecord;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * 文件记录 PO DTO 转换器
 *
 * @author xiaozhanke
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface FileRecordPoDtoMapper extends BasePoDtoMapper<FileRecord, FileRecordDto> {
} 