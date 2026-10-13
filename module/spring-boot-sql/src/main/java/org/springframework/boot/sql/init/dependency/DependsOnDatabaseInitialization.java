/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.sql.init.dependency;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Bean;

/**
 * Indicate that a bean's creation and initialization depends upon database initialization
 * having completed. May be used on a bean's class or its {@link Bean @Bean} definition.
 *
 * @author Andy Wilkinson
 * @since 2.5.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DependsOnDatabaseInitialization {

}
