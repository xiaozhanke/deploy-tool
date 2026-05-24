package com.xiaozhanke.deploy.model.mapper;

import com.xiaozhanke.deploy.model.base.BasePoVoMapper;
import com.xiaozhanke.deploy.model.entity.FileRecord;
import com.xiaozhanke.deploy.model.vo.FileRecordVo;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * 文件记录 PO VO 转换器
 *
 * @author xiaozhanke
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface FileRecordPoVoMapper extends BasePoVoMapper<FileRecord, FileRecordVo> {
}
