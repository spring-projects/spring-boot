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

package org.springframework.boot.configurationprocessor;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import org.springframework.boot.configurationprocessor.model.AnnotationReference;
import org.springframework.boot.configurationprocessor.model.Declaration;
import org.springframework.boot.configurationprocessor.model.MethodDeclaration;
import org.springframework.boot.configurationprocessor.model.TypeDeclaration;
import org.springframework.boot.configurationprocessor.model.TypeReference;
import org.springframework.boot.configurationprocessor.model.VariableDeclaration;

/**
 * {@link PropertyDescriptor} created from a constructor or record parameter.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
abstract class ParameterPropertyDescriptor extends PropertyDescriptor {

	private final VariableDeclaration parameter;

	ParameterPropertyDescriptor(String name, TypeReference type, VariableDeclaration parameter,
			TypeDeclaration declaringElement, MethodDeclaration getter) {
		super(name, type, declaringElement, getter);
		this.parameter = parameter;

	}

	@Override
	protected Declaration getSourceElement() {
		return getParameter();
	}

	final VariableDeclaration getParameter() {
		return this.parameter;
	}

	@Override
	protected Object resolveDefaultValue(MetadataGenerationEnvironment environment) {
		Object defaultValue = getDefaultValueFromAnnotation(environment, getParameter());
		if (defaultValue != null || getParameter().hasDefaultValue()) {
			return defaultValue;
		}
		TypeReference parameterType = getParameter().getType();
		Primitive primitive = (parameterType.isPrimitive()) ? getPrimitive(parameterType) : null;
		return (primitive != null) ? primitive.getDefaultValue() : null;
	}

	private Object getDefaultValueFromAnnotation(MetadataGenerationEnvironment environment, Declaration element) {
		AnnotationReference annotation = environment.getDefaultValueAnnotation(element);
		List<String> defaultValue = getDefaultValue(environment, annotation);
		if (defaultValue != null) {
			Primitive primitive = getPrimitive(determineSpecificType());
			try {
				List<Object> coerced = defaultValue.stream().map((value) -> coerceValue(primitive, value)).toList();
				return (coerced.size() != 1) ? coerced : coerced.get(0);
			}
			catch (IllegalArgumentException ex) {
				environment.getContext().error(ex.getMessage(), element, annotation);
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private List<String> getDefaultValue(MetadataGenerationEnvironment environment, AnnotationReference annotation) {
		if (annotation == null) {
			return null;
		}
		Map<String, Object> values = environment.getAnnotationElementValues(annotation);
		return (List<String>) values.get("value");
	}

	private TypeReference determineSpecificType() {
		TypeReference parameterType = getParameter().getType();
		TypeReference elementType = parameterType.getElementType();
		return (elementType != null) ? elementType : parameterType;
	}

	private Primitive getPrimitive(TypeReference type) {
		String wrapperName = (type.isPrimitive()) ? type.getName(getDeclaringElement()) : type.toString();
		for (Primitive primitive : Primitive.values()) {
			if (primitive.getWrapperName().equals(wrapperName)) {
				return primitive;
			}
		}
		return null;
	}

	private Object coerceValue(Primitive primitive, String value) {
		return (primitive != null) ? primitive.coerce(value) : value;
	}

	@Override
	public boolean isProperty(MetadataGenerationEnvironment env) {
		return !isNested(env); // We must be able to bind it to build the object.
	}

	/**
	 * The primitive types, with their default value and how to coerce a value to them.
	 */
	private enum Primitive {

		BOOLEAN(Boolean.class, false, Boolean::parseBoolean),

		BYTE(Byte.class, (byte) 0, Byte::parseByte),

		SHORT(Short.class, (short) 0, Short::parseShort),

		INT(Integer.class, 0, Integer::parseInt),

		LONG(Long.class, 0L, Long::parseLong),

		CHAR(Character.class, null, Primitive::parseCharacter),

		FLOAT(Float.class, 0F, Float::parseFloat),

		DOUBLE(Double.class, 0D, Double::parseDouble);

		private final String wrapperName;

		private final Object defaultValue;

		private final Function<String, Object> parser;

		Primitive(Class<?> wrapperType, Object defaultValue, Function<String, Object> parser) {
			this.wrapperName = wrapperType.getName();
			this.defaultValue = defaultValue;
			this.parser = parser;
		}

		String getWrapperName() {
			return this.wrapperName;
		}

		Object getDefaultValue() {
			return this.defaultValue;
		}

		Object coerce(String value) {
			try {
				return this.parser.apply(value);
			}
			catch (NumberFormatException ex) {
				throw new IllegalArgumentException(
						String.format("Invalid %s representation '%s'", name().toLowerCase(Locale.ROOT), value));
			}
		}

		private static Object parseCharacter(String value) {
			if (value.length() > 1) {
				throw new IllegalArgumentException(String.format("Invalid character representation '%s'", value));
			}
			return value;
		}

	}

}
