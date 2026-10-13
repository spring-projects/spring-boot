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
import java.time.temporal.ChronoUnit;

/**
 * Annotation that can be used to change the default unit used when converting a
 * {@link Period}.
 *
 * @author Eddú Meléndez
 * @author Edson Chávez
 * @since 2.3.0
 */
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PeriodUnit {

	/**
	 * The Period unit to use if one is not specified.
	 * @return the Period unit
	 */
	ChronoUnit value();

}
