package com.xiaozhanke.deploy.model.base;

import java.util.List;

/**
 * 基础 PO DTO 转换器接口
 *
 * @author xiaozhanke
 */
public interface BasePoDtoMapper<Po, Dto> {
    /**
     * PO 转 DTO
     *
     * @param po PO 类
     * @return DTO 类
     */
    Dto poToDto(Po po);

    /**
     * DTO 转 PO
     *
     * @param dto DTO 类
     * @return PO 类
     */
    Po dtoToPo(Dto dto);

    /**
     * PO List 转 DTO List
     *
     * @param poList PO List
     * @return DTO List
     */
    List<Dto> poListToDtoList(List<Po> poList);

    /**
     * DTO List 转 PO List
     *
     * @param dtoList DTO List
     * @return PO List
     */
    List<Po> dtoListToPoList(List<Dto> dtoList);
}
