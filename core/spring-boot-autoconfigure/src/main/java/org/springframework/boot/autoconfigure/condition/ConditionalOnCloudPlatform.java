/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.condition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.context.annotation.Conditional;

/**
 * {@link Conditional @Conditional} that matches when the specified cloud platform is
 * active.
 *
 * @author Madhura Bhave
 * @since 1.5.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnCloudPlatformCondition.class)
public @interface ConditionalOnCloudPlatform {

	/**
	 * The {@link CloudPlatform cloud platform} that must be active.
	 * @return the expected cloud platform
	 */
	CloudPlatform value();

}
