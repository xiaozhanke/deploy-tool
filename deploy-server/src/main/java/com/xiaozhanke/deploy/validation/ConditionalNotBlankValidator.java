package com.xiaozhanke.deploy.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.util.StringUtils;

import java.util.Arrays;

/**
 * {@link ConditionalNotBlank} 注解的校验逻辑实现类
 *
 * @author xiaozhanke
 */
public class ConditionalNotBlankValidator implements ConstraintValidator<ConditionalNotBlank, Object> {

    /**
     * 触发条件的字段名
     */
    private String selected;
    /**
     * 需要校验的字段名
     */
    private String[] required;
    /**
     * 触发条件的值
     */
    private String[] values;

    /**
     * 初始化校验器，从注解中获取配置参数。
     *
     * @param constraintAnnotation 注解实例
     */
    @Override
    public void initialize(ConditionalNotBlank constraintAnnotation) {
        this.selected = constraintAnnotation.selected();
        this.required = constraintAnnotation.required();
        this.values = constraintAnnotation.values();
    }

    /**
     * 执行核心的校验逻辑。
     *
     * @param object  被校验的整个对象
     * @param context 校验上下文，用于构建错误信息
     * @return 如果校验通过则返回 true，否则返回 false
     */
    @Override
    public boolean isValid(Object object, ConstraintValidatorContext context) {
        // 使用 Spring 的 BeanWrapper 来方便地访问对象属性
        BeanWrapperImpl wrapper = new BeanWrapperImpl(object);
        Object selectedValue = wrapper.getPropertyValue(this.selected);
        // 检查 "if" 条件是否满足：即 `selected` 字段的当前值是否在 `values` 列表中
        boolean isConditionMet = Arrays.stream(this.values)
                .anyMatch(val -> val.equals(String.valueOf(selectedValue)));

        if (isConditionMet) {
            // 如果条件满足，则遍历所有 "then" 字段，检查它们是否为空白
            for (String propName : this.required) {
                Object requiredValue = wrapper.getPropertyValue(propName);
                // 如果发现一个必填字段为空白，则校验失败
                if (requiredValue == null || !StringUtils.hasText(String.valueOf(requiredValue))) {
                    // 禁用默认的类级别错误信息
                    context.disableDefaultConstraintViolation();
                    // 将错误信息精确地附加到具体的字段上
                    context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                            .addPropertyNode(propName)
                            .addConstraintViolation();
                    // 返回失败
                    return false;
                }
            }
        }
        // 如果条件不满足，或者条件满足且所有必填字段都已填写，则校验通过
        return true;
    }
}