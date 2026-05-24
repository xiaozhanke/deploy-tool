package com.xiaozhanke.deploy.model.mapper;

import com.xiaozhanke.deploy.model.base.BasePoVoMapper;
import com.xiaozhanke.deploy.model.entity.DeploymentRecord;
import com.xiaozhanke.deploy.model.vo.DeploymentRecordVo;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * 部署记录 PO VO 转换器
 *
 * @author xiaozhanke
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DeploymentRecordPoVoMapper extends BasePoVoMapper<DeploymentRecord, DeploymentRecordVo> {
}
