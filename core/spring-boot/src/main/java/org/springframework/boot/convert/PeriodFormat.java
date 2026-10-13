/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.convert;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Period;

/**
 * Annotation that can be used to indicate the format to use when converting a
 * {@link Period}.
 *
 * @author Eddú Meléndez
 * @author Edson Chávez
 * @since 2.3.0
 */
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PeriodFormat {

	/**
	 * The {@link Period} format style.
	 * @return the period format style.
	 */
	PeriodStyle value();

}
