/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testsupport.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Improves JUnit5's {@link org.junit.jupiter.api.condition.DisabledOnOs} by adding an
 * architecture check.
 *
 * @author Moritz Halbritter
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(DisabledOnOsCondition.class)
public @interface DisabledOnOs {

	/**
	 * The operating systems on which the annotated class or method should be disabled.
	 * @return the operating systems where the test is disabled
	 */
	OS[] value() default {};

	/**
	 * The operating systems on which the annotated class or method should be disabled.
	 * @return the operating systems where the test is disabled
	 */
	OS[] os() default {};

	/**
	 * The architectures on which the annotated class or method should be disabled.
	 * @return the architectures where the test is disabled
	 */
	String[] architecture() default {};

	/**
	 * See {@link org.junit.jupiter.api.condition.DisabledOnOs#disabledReason()}.
	 * @return disabled reason
	 */
	String disabledReason() default "";

}
