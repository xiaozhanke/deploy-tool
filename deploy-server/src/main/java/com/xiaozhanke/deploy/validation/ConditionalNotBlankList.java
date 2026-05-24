package com.xiaozhanke.deploy.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@link ConditionalNotBlank} 注解的容器
 *
 * @author xiaozhanke
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ConditionalNotBlankList {
    /**
     * 包含一个 {@code ConditionalNotBlank} 注解的数组
     */
    ConditionalNotBlank[] value();
}