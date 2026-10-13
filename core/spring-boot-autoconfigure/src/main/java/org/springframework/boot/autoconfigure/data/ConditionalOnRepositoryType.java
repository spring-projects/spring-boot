/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.data;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * {@link Conditional @Conditional} that only matches when a particular type of Spring
 * Data repository has been enabled.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
@Conditional(OnRepositoryTypeCondition.class)
public @interface ConditionalOnRepositoryType {

	/**
	 * The name of the store that backs the repositories.
	 * @return the store
	 */
	String store();

	/**
	 * The required repository type.
	 * @return the required repository type
	 */
	RepositoryType type();

}
