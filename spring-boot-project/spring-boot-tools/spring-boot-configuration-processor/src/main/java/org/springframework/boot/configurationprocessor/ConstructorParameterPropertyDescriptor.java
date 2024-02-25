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

package org.springframework.boot.configurationprocessor;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

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

	/**
	 * Constructs a new ConstructorParameterPropertyDescriptor with the specified
	 * parameters.
	 * @param ownerElement the TypeElement representing the owner of the property
	 * descriptor
	 * @param factoryMethod the ExecutableElement representing the factory method used to
	 * create the property descriptor
	 * @param source the VariableElement representing the source of the property
	 * descriptor
	 * @param name the name of the property
	 * @param type the TypeMirror representing the type of the property
	 * @param field the VariableElement representing the field associated with the
	 * property
	 * @param getter the ExecutableElement representing the getter method for the property
	 * @param setter the ExecutableElement representing the setter method for the property
	 */
	ConstructorParameterPropertyDescriptor(TypeElement ownerElement, ExecutableElement factoryMethod,
			VariableElement source, String name, TypeMirror type, VariableElement field, ExecutableElement getter,
			ExecutableElement setter) {
		super(ownerElement, factoryMethod, source, name, type, field, getter, setter);
	}

	/**
	 * Determines if the given property is a constructor parameter.
	 * @param env the metadata generation environment
	 * @return {@code true} if the property is not nested, {@code false} otherwise
	 */
	@Override
	protected boolean isProperty(MetadataGenerationEnvironment env) {
		// If it's a constructor parameter, it doesn't matter as we must be able to bind
		// it to build the object.
		return !isNested(env);
	}

	/**
	 * Resolves the default value for the constructor parameter.
	 * @param environment the metadata generation environment
	 * @return the resolved default value
	 */
	@Override
	protected Object resolveDefaultValue(MetadataGenerationEnvironment environment) {
		Object defaultValue = getDefaultValueFromAnnotation(environment, getSource());
		if (defaultValue != null) {
			return defaultValue;
		}
		return getSource().asType().accept(DefaultPrimitiveTypeVisitor.INSTANCE, null);
	}

	/**
	 * Retrieves the default value from the annotation for the given element.
	 * @param environment the metadata generation environment
	 * @param element the element for which to retrieve the default value
	 * @return the default value, or null if not found
	 */
	private Object getDefaultValueFromAnnotation(MetadataGenerationEnvironment environment, Element element) {
		AnnotationMirror annotation = environment.getDefaultValueAnnotation(element);
		List<String> defaultValue = getDefaultValue(environment, annotation);
		if (defaultValue != null) {
			try {
				TypeMirror specificType = determineSpecificType(environment);
				if (defaultValue.size() == 1) {
					return coerceValue(specificType, defaultValue.get(0));
				}
				return defaultValue.stream().map((value) -> coerceValue(specificType, value)).toList();
			}
			catch (IllegalArgumentException ex) {
				environment.getMessager().printMessage(Kind.ERROR, ex.getMessage(), element, annotation);
			}
		}
		return null;
	}

	/**
	 * Retrieves the default value for the given annotation.
	 * @param environment the metadata generation environment
	 * @param annotation the annotation mirror
	 * @return the default value as a list of strings, or null if the annotation is null
	 */
	@SuppressWarnings("unchecked")
	private List<String> getDefaultValue(MetadataGenerationEnvironment environment, AnnotationMirror annotation) {
		if (annotation == null) {
			return null;
		}
		Map<String, Object> values = environment.getAnnotationElementValues(annotation);
		return (List<String>) values.get("value");
	}

	/**
	 * Determines the specific type of the constructor parameter.
	 * @param environment the metadata generation environment
	 * @return the specific type of the constructor parameter
	 */
	private TypeMirror determineSpecificType(MetadataGenerationEnvironment environment) {
		TypeMirror candidate = getSource().asType();
		TypeMirror elementCandidate = environment.getTypeUtils().extractElementType(candidate);
		if (elementCandidate != null) {
			candidate = elementCandidate;
		}
		PrimitiveType primitiveType = environment.getTypeUtils().getPrimitiveType(candidate);
		return (primitiveType != null) ? primitiveType : candidate;
	}

	/**
	 * Coerces the given value to the specified type.
	 * @param type the type to which the value should be coerced
	 * @param value the value to be coerced
	 * @return the coerced value if successful, otherwise the original value
	 */
	private Object coerceValue(TypeMirror type, String value) {
		Object coercedValue = type.accept(DefaultValueCoercionTypeVisitor.INSTANCE, value);
		return (coercedValue != null) ? coercedValue : value;
	}

	/**
	 * DefaultValueCoercionTypeVisitor class.
	 */
	private static final class DefaultValueCoercionTypeVisitor extends TypeKindVisitor8<Object, String> {

		private static final DefaultValueCoercionTypeVisitor INSTANCE = new DefaultValueCoercionTypeVisitor();

		/**
		 * Parses a string representation of a number into the specified number type.
		 * @param value the string representation of the number
		 * @param parser the function used to parse the string into the number type
		 * @param primitiveType the primitive type of the number
		 * @return the parsed number
		 * @throws IllegalArgumentException if the string representation is invalid
		 */
		private <T extends Number> T parseNumber(String value, Function<String, T> parser,
				PrimitiveType primitiveType) {
			try {
				return parser.apply(value);
			}
			catch (NumberFormatException ex) {
				throw new IllegalArgumentException(
						String.format("Invalid %s representation '%s'", primitiveType, value));
			}
		}

		/**
		 * Visits a primitive type as a boolean and returns the corresponding boolean
		 * value.
		 * @param t the primitive type being visited
		 * @param value the string representation of the value
		 * @return the boolean value of the given string representation
		 */
		@Override
		public Object visitPrimitiveAsBoolean(PrimitiveType t, String value) {
			return Boolean.parseBoolean(value);
		}

		/**
		 * Visits a primitive type as a byte and returns the parsed value.
		 * @param t the primitive type to visit
		 * @param value the string value to parse
		 * @return the parsed byte value
		 */
		@Override
		public Object visitPrimitiveAsByte(PrimitiveType t, String value) {
			return parseNumber(value, Byte::parseByte, t);
		}

		/**
		 * Visits a primitive type as a short value.
		 * @param t the primitive type to visit
		 * @param value the string representation of the value
		 * @return the parsed short value
		 */
		@Override
		public Object visitPrimitiveAsShort(PrimitiveType t, String value) {
			return parseNumber(value, Short::parseShort, t);
		}

		/**
		 * Visits a primitive type as an integer value.
		 * @param t the primitive type being visited
		 * @param value the string representation of the value
		 * @return the parsed integer value
		 */
		@Override
		public Object visitPrimitiveAsInt(PrimitiveType t, String value) {
			return parseNumber(value, Integer::parseInt, t);
		}

		/**
		 * Visits a primitive type as a long value.
		 * @param t the primitive type to visit
		 * @param value the string representation of the value
		 * @return the parsed long value
		 */
		@Override
		public Object visitPrimitiveAsLong(PrimitiveType t, String value) {
			return parseNumber(value, Long::parseLong, t);
		}

		/**
		 * Visits a primitive type as a character representation.
		 * @param t the primitive type being visited
		 * @param value the character representation of the primitive type
		 * @return the character representation of the primitive type
		 * @throws IllegalArgumentException if the character representation is invalid
		 */
		@Override
		public Object visitPrimitiveAsChar(PrimitiveType t, String value) {
			if (value.length() > 1) {
				throw new IllegalArgumentException(String.format("Invalid character representation '%s'", value));
			}
			return value;
		}

		/**
		 * Visits a primitive type as a float value.
		 * @param t the primitive type being visited
		 * @param value the string representation of the float value
		 * @return the parsed float value
		 */
		@Override
		public Object visitPrimitiveAsFloat(PrimitiveType t, String value) {
			return parseNumber(value, Float::parseFloat, t);
		}

		/**
		 * Visits a primitive type as a double value.
		 * @param t the primitive type to visit
		 * @param value the string representation of the value
		 * @return the parsed double value
		 */
		@Override
		public Object visitPrimitiveAsDouble(PrimitiveType t, String value) {
			return parseNumber(value, Double::parseDouble, t);
		}

	}

	/**
	 * DefaultPrimitiveTypeVisitor class.
	 */
	private static final class DefaultPrimitiveTypeVisitor extends TypeKindVisitor8<Object, Void> {

		private static final DefaultPrimitiveTypeVisitor INSTANCE = new DefaultPrimitiveTypeVisitor();

		/**
		 * Visits a primitive type and returns a boolean value.
		 * @param t the primitive type to visit
		 * @param ignore a void parameter to be ignored
		 * @return false
		 */
		@Override
		public Object visitPrimitiveAsBoolean(PrimitiveType t, Void ignore) {
			return false;
		}

		/**
		 * Visits a primitive type as a byte.
		 * @param t the primitive type to visit
		 * @param ignore a parameter to ignore (null)
		 * @return the byte representation of the primitive type
		 */
		@Override
		public Object visitPrimitiveAsByte(PrimitiveType t, Void ignore) {
			return (byte) 0;
		}

		/**
		 * Visits a primitive type and returns a short value.
		 * @param t the primitive type to visit
		 * @param ignore a void parameter to be ignored
		 * @return a short value
		 */
		@Override
		public Object visitPrimitiveAsShort(PrimitiveType t, Void ignore) {
			return (short) 0;
		}

		/**
		 * Visits a primitive type and returns an integer value.
		 * @param t the primitive type to visit
		 * @param ignore a void parameter to be ignored
		 * @return the integer value representing the visited primitive type
		 */
		@Override
		public Object visitPrimitiveAsInt(PrimitiveType t, Void ignore) {
			return 0;
		}

		/**
		 * Visits a primitive type and returns a long value.
		 * @param t the primitive type to visit
		 * @param ignore a void parameter to be ignored
		 * @return the long value representing the primitive type
		 */
		@Override
		public Object visitPrimitiveAsLong(PrimitiveType t, Void ignore) {
			return 0L;
		}

		/**
		 * Visits a primitive type as a char.
		 * @param t the primitive type to visit
		 * @param ignore a parameter to ignore (can be null)
		 * @return null
		 */
		@Override
		public Object visitPrimitiveAsChar(PrimitiveType t, Void ignore) {
			return null;
		}

		/**
		 * Visits a primitive type and returns a float value.
		 * @param t the primitive type to visit
		 * @param ignore a void parameter to ignore
		 * @return the float value
		 */
		@Override
		public Object visitPrimitiveAsFloat(PrimitiveType t, Void ignore) {
			return 0F;
		}

		/**
		 * Visits a primitive type and returns a double value.
		 * @param t the primitive type to visit
		 * @param ignore a void parameter to be ignored
		 * @return the double value
		 */
		@Override
		public Object visitPrimitiveAsDouble(PrimitiveType t, Void ignore) {
			return 0D;
		}

	}

}
