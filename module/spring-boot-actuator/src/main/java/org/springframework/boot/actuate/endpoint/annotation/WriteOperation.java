/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.aot.hint.annotation.Reflective;
import org.springframework.boot.actuate.endpoint.Producible;

/**
 * Identifies a method on an {@link Endpoint @Endpoint} as being a write operation.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Reflective(OperationReflectiveProcessor.class)
public @interface WriteOperation {

	/**
	 * The media type of the result of the operation.
	 * @return the media type
	 */
	String[] produces() default {};

	/**
	 * The media types of the result of the operation.
	 * @return the media types
	 */
	@SuppressWarnings("rawtypes")
	Class<? extends Producible> producesFrom() default Producible.class;

}
