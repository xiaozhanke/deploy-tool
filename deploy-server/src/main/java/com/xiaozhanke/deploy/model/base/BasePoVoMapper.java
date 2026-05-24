package com.xiaozhanke.deploy.model.base;

import java.util.List;

/**
 * 基础 PO VO 转换器接口
 *
 * @author xiaozhanke
 */
public interface BasePoVoMapper<Po, Vo> {
    /**
     * PO 转 VO
     *
     * @param po PO 类
     * @return VO 类
     */
    Vo poToVo(Po po);

    /**
     * VO 转 PO
     *
     * @param vo VO 类
     * @return PO 类
     */
    Po voToPo(Vo vo);

    /**
     * PO List 转 VO List
     *
     * @param poList PO List
     * @return VO List
     */
    List<Vo> poListToVoList(List<Po> poList);

    /**
     * VO List 转 PO List
     *
     * @param voList VO List
     * @return PO List
     */
    List<Po> voListToPoList(List<Vo> voList);
}
