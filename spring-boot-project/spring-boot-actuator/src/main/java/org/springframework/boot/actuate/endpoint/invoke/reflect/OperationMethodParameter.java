/*
 * Copyright 2012-2024 the original author or authors.
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

import javax.annotation.Nonnull;
import javax.annotation.meta.When;

import org.springframework.boot.actuate.endpoint.invoke.OperationParameter;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * {@link OperationParameter} created from an {@link OperationMethod}.
 *
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
class OperationMethodParameter implements OperationParameter {

	private static final boolean jsr305Present = ClassUtils.isPresent("javax.annotation.Nonnull", null);

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

	/**
	 * Returns the name of the OperationMethodParameter.
	 * @return the name of the OperationMethodParameter
	 */
	@Override
	public String getName() {
		return this.name;
	}

	/**
	 * Returns the type of the parameter.
	 * @return the type of the parameter
	 */
	@Override
	public Class<?> getType() {
		return this.parameter.getType();
	}

	/**
	 * Returns a boolean value indicating whether the parameter is mandatory.
	 * @return true if the parameter is mandatory, false otherwise
	 */
	@Override
	public boolean isMandatory() {
		if (!ObjectUtils.isEmpty(this.parameter.getAnnotationsByType(Nullable.class))) {
			return false;
		}
		if (jsr305Present) {
			return new Jsr305().isMandatory(this.parameter);
		}
		return true;
	}

	/**
	 * Returns the annotation of the specified type if present on this parameter.
	 * @param annotation the class object representing the annotation type
	 * @return the annotation of the specified type if present, or null if not present
	 * @throws NullPointerException if the specified annotation is null
	 * @throws TypeNotPresentException if the annotation type is not accessible
	 * @throws AnnotationTypeMismatchException if the annotation is not of the specified
	 * type
	 */
	@Override
	public <T extends Annotation> T getAnnotation(Class<T> annotation) {
		return this.parameter.getAnnotation(annotation);
	}

	/**
	 * Returns a string representation of the OperationMethodParameter object. The string
	 * representation includes the name of the parameter and its type.
	 * @return a string representation of the OperationMethodParameter object
	 */
	@Override
	public String toString() {
		return this.name + " of type " + this.parameter.getType().getName();
	}

	/**
	 * Jsr305 class.
	 */
	private static final class Jsr305 {

		/**
		 * Checks if a parameter is mandatory based on the presence of the {@code Nonnull}
		 * annotation and its {@code when} value.
		 * @param parameter the parameter to check
		 * @return {@code true} if the parameter is mandatory, {@code false} otherwise
		 */
		boolean isMandatory(Parameter parameter) {
			MergedAnnotation<Nonnull> annotation = MergedAnnotations.from(parameter).get(Nonnull.class);
			return !annotation.isPresent() || annotation.getEnum("when", When.class) == When.ALWAYS;
		}

	}

}
