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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

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

	private final ExecutableElement setter;

	private final VariableElement field;

	private final ExecutableElement factoryMethod;

	LombokPropertyDescriptor(String name, TypeMirror type, TypeElement declaringElement, ExecutableElement getter,
			ExecutableElement setter, VariableElement field, ExecutableElement factoryMethod) {
		super(name, type, declaringElement, getter);
		this.factoryMethod = factoryMethod;
		this.field = field;
		this.setter = setter;
	}

	VariableElement getField() {
		return this.field;
	}

	@Override
	protected boolean isMarkedAsNested(MetadataGenerationEnvironment environment) {
		return environment.getNestedConfigurationPropertyAnnotation(getField()) != null;
	}

	@Override
	protected String resolveDescription(MetadataGenerationEnvironment environment) {
		return environment.getTypeUtils().getJavaDoc(this.field);
	}

	@Override
	protected Object resolveDefaultValue(MetadataGenerationEnvironment environment) {
		return environment.getFieldDefaultValue(getDeclaringElement(), getName());
	}

	@Override
	protected List<Element> getDeprecatableElements() {
		return Arrays.asList(getGetter(), this.setter, this.field, this.factoryMethod);
	}

	@Override
	public boolean isProperty(MetadataGenerationEnvironment env) {
		if (!hasLombokPublicAccessor(env, true)) {
			return false;
		}
		boolean isCollection = env.getTypeUtils().isCollectionOrMap(getType());
		return !env.isExcluded(getType()) && (hasSetter(env) || isCollection);
	}

	@Override
	public boolean isNested(MetadataGenerationEnvironment environment) {
		return hasLombokPublicAccessor(environment, true) && super.isNested(environment);
	}

	private boolean hasSetter(MetadataGenerationEnvironment env) {
		boolean nonFinalPublicField = !getField().getModifiers().contains(Modifier.FINAL)
				&& hasLombokPublicAccessor(env, false);
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
		AnnotationMirror lombokMethodAnnotationOnField = env.getAnnotation(getField(), annotation);
		if (lombokMethodAnnotationOnField != null) {
			return isAccessLevelPublic(env, lombokMethodAnnotationOnField);
		}
		AnnotationMirror lombokMethodAnnotationOnElement = env.getAnnotation(getDeclaringElement(), annotation);
		if (lombokMethodAnnotationOnElement != null) {
			return isAccessLevelPublic(env, lombokMethodAnnotationOnElement);
		}
		return (env.hasAnnotation(getDeclaringElement(), LOMBOK_DATA_ANNOTATION)
				|| env.hasAnnotation(getDeclaringElement(), LOMBOK_VALUE_ANNOTATION));
	}

	private boolean isAccessLevelPublic(MetadataGenerationEnvironment env, AnnotationMirror lombokAnnotation) {
		Map<String, Object> values = env.getAnnotationElementValues(lombokAnnotation);
		Object value = values.get("value");
		return (value == null || value.toString().equals(LOMBOK_ACCESS_LEVEL_PUBLIC));
	}

}
