package com.xiaozhanke.deploy.model.validation;

/**
 * 通用验证组定义
 *
 * @author xiaozhanke
 */
public interface ValidationGroups {

    // 创建操作验证
    interface Create {
    }

    // 更新操作验证
    interface Update {
    }

    // 部分更新验证
    interface Patch {
    }

    // 删除操作验证
    interface Delete {
    }

    // 查询操作验证
    interface Query {
    }
}