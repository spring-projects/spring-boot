/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Alternative to Spring Boot's {@code @DeprecatedConfigurationProperty} for testing
 * (removes the need for a dependency on the real annotation).
 *
 * @author Phillip Webb
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TestDeprecatedConfigurationProperty {

	/**
	 * The reason for the deprecation.
	 * @return the deprecation reason
	 */
	String reason() default "";

	/**
	 * The field that should be used instead (if any).
	 * @return the replacement field
	 */
	String replacement() default "";

	/**
	 * The version in which the property became deprecated.
	 * @return the version
	 */
	String since() default "";

}
