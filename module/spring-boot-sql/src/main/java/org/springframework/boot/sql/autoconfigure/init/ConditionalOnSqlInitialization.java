/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.sql.autoconfigure.init;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.sql.init.DatabaseInitializationMode;
import org.springframework.context.annotation.Conditional;

/**
 * Condition that matches when {@code spring.sql.init.mode} is set to a value other than
 * {@link DatabaseInitializationMode#NEVER}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 4.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Conditional(OnSqlInitializationCondition.class)
public @interface ConditionalOnSqlInitialization {

}
