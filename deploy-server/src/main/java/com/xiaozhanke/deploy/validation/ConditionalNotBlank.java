package com.xiaozhanke.deploy.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注释注解，条件性非空检查
 *
 * @author xiaozhanke
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ConditionalNotBlankValidator.class)
@Repeatable(ConditionalNotBlankList.class)
public @interface ConditionalNotBlank {

    /**
     * 校验失败时返回的错误消息。
     */
    String message() default "字段不能为空";

    /**
     * 用于分组校验
     */
    Class<?>[] groups() default {};

    /**
     * 校验的有效负载
     */
    Class<? extends Payload>[] payload() default {};

    /**
     * 触发条件的字段名
     */
    String selected();

    /**
     * 需要进行非空校验的字段名数组
     */
    String[] required();

    /**
     * 触发条件的字段值数组
     */
    String[] values();
}