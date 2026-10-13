/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.condition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.naming.InitialContext;

import org.springframework.context.annotation.Conditional;

/**
 * {@link Conditional @Conditional} that matches based on the availability of a JNDI
 * {@link InitialContext} and the ability to lookup specific locations.
 *
 * @author Phillip Webb
 * @since 1.2.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnJndiCondition.class)
public @interface ConditionalOnJndi {

	/**
	 * JNDI Locations, one of which must exist. If no locations are specific the condition
	 * matches solely based on the presence of an {@link InitialContext}.
	 * @return the JNDI locations
	 */
	String[] value() default {};

}
