/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.autoconfigure.json;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Conditional;

/**
 * {@link Conditional @Conditional} that checks if JSON testers are enabled.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
@ConditionalOnBooleanProperty("spring.test.jsontesters.enabled")
@ConditionalOnClass(name = "org.assertj.core.api.Assert")
public @interface ConditionalOnJsonTesters {

}
