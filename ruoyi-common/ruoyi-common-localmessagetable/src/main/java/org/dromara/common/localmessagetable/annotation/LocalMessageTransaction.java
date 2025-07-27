package org.dromara.common.localmessagetable.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LocalMessageTransaction {
    /**
     * 消息描述
     */
    String description() default "";

    /**
     * 最大重试次数
     */
    int maxRetryTimes() default 3;
}
