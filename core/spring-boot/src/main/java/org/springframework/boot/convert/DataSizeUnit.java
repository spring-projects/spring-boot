/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.convert;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;

/**
 * Annotation that can be used to change the default unit used when converting a
 * {@link DataSize}.
 *
 * @author Stephane Nicoll
 * @since 2.1.0
 */
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataSizeUnit {

	/**
	 * The {@link DataUnit} to use if one is not specified.
	 * @return the data unit
	 */
	DataUnit value();

}
