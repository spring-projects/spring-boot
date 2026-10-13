/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.convert;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Duration;

/**
 * Annotation that can be used to indicate the format to use when converting a
 * {@link Duration}.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DurationFormat {

	/**
	 * The duration format style.
	 * @return the duration format style.
	 */
	DurationStyle value();

}
