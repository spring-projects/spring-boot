/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context.filter.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.ApplicationContext;

/**
 * Annotation that can be on tests to define a set of {@link TypeExcludeFilter} classes
 * that should be registered with the {@link ApplicationContext}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 * @see TypeExcludeFilter
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface TypeExcludeFilters {

	/**
	 * Specifies {@link TypeExcludeFilter} classes that should be registered. Classes
	 * specified here can either have a no-arg constructor or accept a single
	 * {@code Class<?>} argument if they need access to the {@code testClass}.
	 * @see TypeExcludeFilter
	 * @return the type exclude filters to apply
	 */
	Class<? extends TypeExcludeFilter>[] value();

}
