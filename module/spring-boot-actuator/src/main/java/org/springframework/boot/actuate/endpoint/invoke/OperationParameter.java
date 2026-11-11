/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.invoke;

import java.lang.annotation.Annotation;

import org.jspecify.annotations.Nullable;

/**
 * A single operation parameter.
 *
 * @author Phillip Webb
 * @author Moritz Halbritter
 * @since 2.0.0
 */
public interface OperationParameter {

	/**
	 * Returns the parameter name.
	 * @return the name
	 */
	String getName();

	/**
	 * Returns the parameter type.
	 * @return the type
	 */
	Class<?> getType();

	/**
	 * Return if the parameter is mandatory (does not accept null values).
	 * @return if the parameter is mandatory
	 */
	boolean isMandatory();

	/**
	 * Returns this element's annotation for the specified type if such an annotation is
	 * present, else null.
	 * @param annotation class of the annotation
	 * @return annotation value
	 * @param <T> type of the annotation
	 * @since 2.7.8
	 */
	<T extends Annotation> @Nullable T getAnnotation(Class<T> annotation);

}
