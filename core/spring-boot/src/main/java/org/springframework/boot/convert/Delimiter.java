/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.convert;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a field or method parameter should be converted to collection using the
 * specified delimiter.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE })
public @interface Delimiter {

	/**
	 * A delimiter value used to indicate that no delimiter is required and the result
	 * should be a single element containing the entire string.
	 */
	String NONE = "";

	/**
	 * The delimiter to use or {@code NONE} if the entire contents should be treated as a
	 * single element.
	 * @return the delimiter
	 */
	String value();

}
