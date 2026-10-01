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

import java.util.Map;

import org.springframework.boot.configurationprocessor.metadata.ItemDeprecation;
import org.springframework.boot.configurationprocessor.model.AnnotationReference;
import org.springframework.boot.configurationprocessor.model.Declaration;
import org.springframework.boot.configurationprocessor.model.MethodDeclaration;
import org.springframework.boot.configurationprocessor.model.TypeDeclaration;
import org.springframework.boot.configurationprocessor.model.TypeReference;
import org.springframework.boot.configurationprocessor.model.VariableDeclaration;

/**
 * A {@link PropertyDescriptor} for a Lombok field.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class LombokPropertyDescriptor extends PropertyDescriptor {

	private static final String LOMBOK_DATA_ANNOTATION = "lombok.Data";

	private static final String LOMBOK_VALUE_ANNOTATION = "lombok.Value";

	private static final String LOMBOK_GETTER_ANNOTATION = "lombok.Getter";

	private static final String LOMBOK_SETTER_ANNOTATION = "lombok.Setter";

	private static final String LOMBOK_ACCESS_LEVEL_PUBLIC = "PUBLIC";

	private final MethodDeclaration setter;

	private final VariableDeclaration field;

	private final MethodDeclaration factoryMethod;

	LombokPropertyDescriptor(String name, TypeReference type, TypeDeclaration declaringElement,
			MethodDeclaration getter, MethodDeclaration setter, VariableDeclaration field,
			MethodDeclaration factoryMethod) {
		super(name, type, declaringElement, getter);
		this.factoryMethod = factoryMethod;
		this.field = field;
		this.setter = setter;
	}

	@Override
	protected Declaration getSourceElement() {
		return getField();
	}

	VariableDeclaration getField() {
		return this.field;
	}

	@Override
	protected boolean isMarkedAsNested(MetadataGenerationEnvironment environment) {
		return environment.getNestedConfigurationPropertyAnnotation(getField()) != null;
	}

	@Override
	protected String resolveDescription(MetadataGenerationEnvironment environment) {
		return environment.getDescription(this.field);
	}

	@Override
	protected Object resolveDefaultValue(MetadataGenerationEnvironment environment) {
		return environment.getFieldDefaultValue(getDeclaringElement(), this.field);
	}

	@Override
	protected ItemDeprecation resolveItemDeprecation(MetadataGenerationEnvironment environment) {
		return resolveItemDeprecation(environment, getGetter(), this.setter, this.field, this.factoryMethod);
	}

	@Override
	public boolean isProperty(MetadataGenerationEnvironment env) {
		if (!hasLombokPublicAccessor(env, true)) {
			return false;
		}
		boolean isCollection = getType().isCollectionOrMap();
		return !env.isExcluded(getType()) && (hasSetter(env) || isCollection);
	}

	@Override
	public boolean isNested(MetadataGenerationEnvironment environment) {
		return hasLombokPublicAccessor(environment, true) && super.isNested(environment);
	}

	private boolean hasSetter(MetadataGenerationEnvironment env) {
		boolean nonFinalPublicField = !getField().isFinal() && hasLombokPublicAccessor(env, false);
		return this.setter != null || nonFinalPublicField;
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
		AnnotationReference lombokMethodAnnotationOnField = env.getAnnotation(getField(), annotation);
		if (lombokMethodAnnotationOnField != null) {
			return isAccessLevelPublic(env, lombokMethodAnnotationOnField);
		}
		AnnotationReference lombokMethodAnnotationOnElement = env.getAnnotation(getDeclaringElement(), annotation);
		if (lombokMethodAnnotationOnElement != null) {
			return isAccessLevelPublic(env, lombokMethodAnnotationOnElement);
		}
		return (env.hasAnnotation(getDeclaringElement(), LOMBOK_DATA_ANNOTATION)
				|| env.hasAnnotation(getDeclaringElement(), LOMBOK_VALUE_ANNOTATION));
	}

	private boolean isAccessLevelPublic(MetadataGenerationEnvironment env, AnnotationReference lombokAnnotation) {
		Map<String, Object> values = env.getAnnotationElementValues(lombokAnnotation);
		Object value = values.get("value");
		return (value == null || value.toString().equals(LOMBOK_ACCESS_LEVEL_PUBLIC));
	}

}
