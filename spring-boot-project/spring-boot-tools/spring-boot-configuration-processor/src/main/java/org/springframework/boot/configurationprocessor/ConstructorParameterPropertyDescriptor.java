/*
 * Copyright 2012-2019 the original author or authors.
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
import java.util.Map;
import java.util.stream.Collectors;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.TypeKindVisitor8;
import javax.tools.Diagnostic.Kind;

/**
 * A {@link PropertyDescriptor} for a constructor parameter.
 *
 * @author Stephane Nicoll
 */
class ConstructorParameterPropertyDescriptor extends PropertyDescriptor<VariableElement> {

	ConstructorParameterPropertyDescriptor(TypeElement ownerElement, ExecutableElement factoryMethod,
			VariableElement source, String name, TypeMirror type, VariableElement field, ExecutableElement getter,
			ExecutableElement setter) {
		super(ownerElement, factoryMethod, source, name, type, field, getter, setter);
	}

	@Override
	protected boolean isProperty(MetadataGenerationEnvironment env) {
		// If it's a constructor parameter, it doesn't matter as we must be able to bind
		// it to build the object.
		return !isNested(env);
	}

	@Override
	protected Object resolveDefaultValue(MetadataGenerationEnvironment environment) {
		Object defaultValue = getDefaultValueFromAnnotation(environment, getSource());
		if (defaultValue != null) {
			return defaultValue;
		}
		return getSource().asType().accept(DefaultPrimitiveTypeVisitor.INSTANCE, null);
	}

	private Object getDefaultValueFromAnnotation(MetadataGenerationEnvironment environment, Element element) {
		AnnotationMirror annotation = environment.getDefaultValueAnnotation(element);
		List<String> defaultValue = getDefaultValue(environment, annotation);
		if (defaultValue != null) {
			try {
				TypeMirror specificType = determineSpecificType(environment);
				if (defaultValue.size() == 1) {
					return coerceValue(specificType, defaultValue.get(0));
				}
				return defaultValue.stream().map((value) -> coerceValue(specificType, value))
						.collect(Collectors.toList());
			}
			catch (IllegalArgumentException ex) {
				environment.getMessager().printMessage(Kind.ERROR, ex.getMessage(), element, annotation);
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private List<String> getDefaultValue(MetadataGenerationEnvironment environment, AnnotationMirror annotation) {
		if (annotation == null) {
			return null;
		}
		Map<String, Object> values = environment.getAnnotationElementValues(annotation);
		return (List<String>) values.get("value");
	}

	private TypeMirror determineSpecificType(MetadataGenerationEnvironment environment) {
		TypeMirror candidate = getSource().asType();
		TypeMirror elementCandidate = environment.getTypeUtils().extractElementType(candidate);
		if (elementCandidate != null) {
			candidate = elementCandidate;
		}
		PrimitiveType primitiveType = environment.getTypeUtils().getPrimitiveType(candidate);
		return (primitiveType != null) ? primitiveType : candidate;
	}

	private Object coerceValue(TypeMirror type, String value) {
		Object coercedValue = type.accept(DefaultValueCoercionTypeVisitor.INSTANCE, value);
		return (coercedValue != null) ? coercedValue : value;
	}

	private static class DefaultValueCoercionTypeVisitor extends TypeKindVisitor8<Object, String> {

		private static final DefaultValueCoercionTypeVisitor INSTANCE = new DefaultValueCoercionTypeVisitor();

		private Integer parseInteger(String value) {
			try {
				return Integer.valueOf(value);
			}
			catch (NumberFormatException ex) {
				throw new IllegalArgumentException(String.format("Invalid number representation '%s'", value));
			}
		}

		private Double parseFloatingPoint(String value) {
			try {
				return Double.valueOf(value);
			}
			catch (NumberFormatException ex) {
				throw new IllegalArgumentException(String.format("Invalid floating point representation '%s'", value));
			}
		}

		@Override
		public Object visitPrimitiveAsBoolean(PrimitiveType t, String value) {
			return Boolean.parseBoolean(value);
		}

		@Override
		public Object visitPrimitiveAsByte(PrimitiveType t, String value) {
			return parseInteger(value);
		}

		@Override
		public Object visitPrimitiveAsShort(PrimitiveType t, String value) {
			return parseInteger(value);
		}

		@Override
		public Object visitPrimitiveAsInt(PrimitiveType t, String value) {
			return parseInteger(value);
		}

		@Override
		public Object visitPrimitiveAsLong(PrimitiveType t, String value) {
			return parseInteger(value);
		}

		@Override
		public Object visitPrimitiveAsChar(PrimitiveType t, String value) {
			if (value.length() > 1) {
				throw new IllegalArgumentException(String.format("Invalid character representation '%s'", value));
			}
			return value;
		}

		@Override
		public Object visitPrimitiveAsFloat(PrimitiveType t, String value) {
			return parseFloatingPoint(value);
		}

		@Override
		public Object visitPrimitiveAsDouble(PrimitiveType t, String value) {
			return parseFloatingPoint(value);
		}

	}

	private static class DefaultPrimitiveTypeVisitor extends TypeKindVisitor8<Object, Void> {

		private static final DefaultPrimitiveTypeVisitor INSTANCE = new DefaultPrimitiveTypeVisitor();

		@Override
		public Object visitPrimitiveAsBoolean(PrimitiveType t, Void ignore) {
			return false;
		}

		@Override
		public Object visitPrimitiveAsByte(PrimitiveType t, Void ignore) {
			return 0;
		}

		@Override
		public Object visitPrimitiveAsShort(PrimitiveType t, Void ignore) {
			return 0;
		}

		@Override
		public Object visitPrimitiveAsInt(PrimitiveType t, Void ignore) {
			return 0;
		}

		@Override
		public Object visitPrimitiveAsLong(PrimitiveType t, Void ignore) {
			return 0L;
		}

		@Override
		public Object visitPrimitiveAsChar(PrimitiveType t, Void ignore) {
			return null;
		}

		@Override
		public Object visitPrimitiveAsFloat(PrimitiveType t, Void ignore) {
			return 0;
		}

		@Override
		public Object visitPrimitiveAsDouble(PrimitiveType t, Void ignore) {
			return 0D;
		}

	}

}
