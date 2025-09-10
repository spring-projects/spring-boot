/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.endpoint.invoke.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.util.function.Predicate;

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

	private final Predicate<Parameter> optional;

	/**
	 * Create a new {@link OperationMethodParameter} instance.
	 * @param name the parameter name
	 * @param parameter the parameter
	 * @param optionalParameters predicate to test if a parameter is optional
	 */
	OperationMethodParameter(String name, Parameter parameter, Predicate<Parameter> optionalParameters) {
		this.name = name;
		this.parameter = parameter;
		this.optional = optionalParameters;
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
		return !isOptional();
	}

	private boolean isOptional() {
		return Nullness.NULLABLE == Nullness.forParameter(this.parameter) || this.optional.test(this.parameter);
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
