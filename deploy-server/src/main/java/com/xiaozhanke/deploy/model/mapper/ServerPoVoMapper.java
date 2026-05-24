package com.xiaozhanke.deploy.model.mapper;

import com.xiaozhanke.deploy.model.base.BasePoVoMapper;
import com.xiaozhanke.deploy.model.entity.ServerRecord;
import com.xiaozhanke.deploy.model.vo.ServerRecordVo;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * 服务器 Po Vo 转换器
 *
 * @author xiaozhanke
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ServerPoVoMapper extends BasePoVoMapper<ServerRecord, ServerRecordVo> {
}
