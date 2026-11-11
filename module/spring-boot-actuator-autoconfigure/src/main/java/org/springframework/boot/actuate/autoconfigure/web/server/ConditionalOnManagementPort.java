/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.autoconfigure.web.server;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * {@link Conditional @Conditional} that matches based on the configuration of the
 * management port.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
@Conditional(OnManagementPortCondition.class)
public @interface ConditionalOnManagementPort {

	/**
	 * The {@link ManagementPortType} to match.
	 * @return the port type
	 */
	ManagementPortType value();

}
