/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.condition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * {@link Conditional @Conditional} that only matches when coordinated restore at
 * checkpoint is to be used.
 *
 * @author Andy Wilkinson
 * @since 3.2.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnClass(name = "org.crac.Resource")
public @interface ConditionalOnCheckpointRestore {

}
