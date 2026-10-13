/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.invoke.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;

import org.springframework.boot.actuate.endpoint.invoke.OperationParameter;
import org.springframework.core.Nullness;

/**
 * {@link OperationParameter} created from an {@link OperationMethod}.
 *
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
class OperationMethodParameter implements OperationParameter {

	private final String name;

	private final Parameter parameter;

	/**
	 * Create a new {@link OperationMethodParameter} instance.
	 * @param name the parameter name
	 * @param parameter the parameter
	 */
	OperationMethodParameter(String name, Parameter parameter) {
		this.name = name;
		this.parameter = parameter;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public Class<?> getType() {
		return this.parameter.getType();
	}

	@Override
	public boolean isMandatory() {
		return Nullness.NULLABLE != Nullness.forParameter(this.parameter);
	}

	@Override
	public <T extends Annotation> T getAnnotation(Class<T> annotation) {
		return this.parameter.getAnnotation(annotation);
	}

	@Override
	public String toString() {
		return this.name + " of type " + this.parameter.getType().getName();
	}

}
