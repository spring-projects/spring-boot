/*
 * Copyright 2012-2021 the original author or authors.
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

import java.util.Map;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import org.springframework.boot.configurationprocessor.metadata.ItemDeprecation;

/**
 * A {@link PropertyDescriptor} for a Lombok field.
 *
 * @author Stephane Nicoll
 */
class LombokPropertyDescriptor extends PropertyDescriptor<VariableElement> {

	private static final String LOMBOK_DATA_ANNOTATION = "lombok.Data";

	private static final String LOMBOK_VALUE_ANNOTATION = "lombok.Value";

	private static final String LOMBOK_GETTER_ANNOTATION = "lombok.Getter";

	private static final String LOMBOK_SETTER_ANNOTATION = "lombok.Setter";

	private static final String LOMBOK_ACCESS_LEVEL_PUBLIC = "PUBLIC";

	/**
	 * Constructs a new {@code LombokPropertyDescriptor} with the specified parameters.
	 * @param typeElement the type element representing the class containing the property
	 * @param factoryMethod the factory method used to create instances of the class
	 * @param field the variable element representing the property field
	 * @param name the name of the property
	 * @param type the type of the property
	 * @param getter the getter method for the property
	 * @param setter the setter method for the property
	 */
	LombokPropertyDescriptor(TypeElement typeElement, ExecutableElement factoryMethod, VariableElement field,
			String name, TypeMirror type, ExecutableElement getter, ExecutableElement setter) {
		super(typeElement, factoryMethod, field, name, type, field, getter, setter);
	}

	/**
	 * Checks if the property is valid for metadata generation.
	 * @param env the metadata generation environment
	 * @return true if the property is valid, false otherwise
	 */
	@Override
	protected boolean isProperty(MetadataGenerationEnvironment env) {
		if (!hasLombokPublicAccessor(env, true)) {
			return false;
		}
		boolean isCollection = env.getTypeUtils().isCollectionOrMap(getType());
		return !env.isExcluded(getType()) && (hasSetter(env) || isCollection);
	}

	/**
	 * Resolves the default value for the property.
	 * @param environment the metadata generation environment
	 * @return the default value for the property
	 */
	@Override
	protected Object resolveDefaultValue(MetadataGenerationEnvironment environment) {
		return environment.getFieldDefaultValue(getOwnerElement(), getName());
	}

	/**
	 * Checks if the property is nested.
	 * @param environment the metadata generation environment
	 * @return {@code true} if the property is nested, {@code false} otherwise
	 */
	@Override
	protected boolean isNested(MetadataGenerationEnvironment environment) {
		if (!hasLombokPublicAccessor(environment, true)) {
			return false;
		}
		return super.isNested(environment);
	}

	/**
	 * Resolves the deprecation status of the item associated with this property
	 * descriptor.
	 * @param environment the metadata generation environment
	 * @return the item deprecation information, or null if the item is not deprecated
	 */
	@Override
	protected ItemDeprecation resolveItemDeprecation(MetadataGenerationEnvironment environment) {
		boolean deprecated = environment.isDeprecated(getField()) || environment.isDeprecated(getGetter())
				|| environment.isDeprecated(getFactoryMethod());
		return deprecated ? environment.resolveItemDeprecation(getGetter()) : null;
	}

	/**
	 * Checks if the property has a setter method or a non-final public field.
	 * @param env the metadata generation environment
	 * @return {@code true} if the property has a setter method or a non-final public
	 * field, {@code false} otherwise
	 */
	private boolean hasSetter(MetadataGenerationEnvironment env) {
		boolean nonFinalPublicField = !getField().getModifiers().contains(Modifier.FINAL)
				&& hasLombokPublicAccessor(env, false);
		return getSetter() != null || nonFinalPublicField;
	}

	/**
	 * Determine if the current {@link #getField() field} defines a public accessor using
	 * lombok annotations.
	 * @param env the {@link MetadataGenerationEnvironment}
	 * @param getter {@code true} to look for the read accessor, {@code false} for the
	 * write accessor
	 * @return {@code true} if this field has a public accessor of the specified type
	 */
	private boolean hasLombokPublicAccessor(MetadataGenerationEnvironment env, boolean getter) {
		String annotation = (getter ? LOMBOK_GETTER_ANNOTATION : LOMBOK_SETTER_ANNOTATION);
		AnnotationMirror lombokMethodAnnotationOnField = env.getAnnotation(getField(), annotation);
		if (lombokMethodAnnotationOnField != null) {
			return isAccessLevelPublic(env, lombokMethodAnnotationOnField);
		}
		AnnotationMirror lombokMethodAnnotationOnElement = env.getAnnotation(getOwnerElement(), annotation);
		if (lombokMethodAnnotationOnElement != null) {
			return isAccessLevelPublic(env, lombokMethodAnnotationOnElement);
		}
		return (env.hasAnnotation(getOwnerElement(), LOMBOK_DATA_ANNOTATION)
				|| env.hasAnnotation(getOwnerElement(), LOMBOK_VALUE_ANNOTATION));
	}

	/**
	 * Checks if the access level of a Lombok annotation is public.
	 * @param env the metadata generation environment
	 * @param lombokAnnotation the Lombok annotation to check
	 * @return true if the access level is public, false otherwise
	 */
	private boolean isAccessLevelPublic(MetadataGenerationEnvironment env, AnnotationMirror lombokAnnotation) {
		Map<String, Object> values = env.getAnnotationElementValues(lombokAnnotation);
		Object value = values.get("value");
		return (value == null || value.toString().equals(LOMBOK_ACCESS_LEVEL_PUBLIC));
	}

}
