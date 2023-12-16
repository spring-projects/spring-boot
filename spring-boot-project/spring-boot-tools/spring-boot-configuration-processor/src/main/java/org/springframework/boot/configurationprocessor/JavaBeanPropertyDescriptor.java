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

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

/**
 * A {@link PropertyDescriptor} for a standard JavaBean property.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class JavaBeanPropertyDescriptor extends PropertyDescriptor {

	private final ExecutableElement setter;

	private final VariableElement field;

	private final ExecutableElement factoryMethod;

	JavaBeanPropertyDescriptor(String name, TypeMirror type, TypeElement declaringElement, ExecutableElement getter,
			ExecutableElement setter, VariableElement field, ExecutableElement factoryMethod) {
		super(name, type, declaringElement, getter);
		this.setter = setter;
		this.field = field;
		this.factoryMethod = factoryMethod;
	}

	ExecutableElement getSetter() {
		return this.setter;
	}

	@Override
	protected boolean isMarkedAsNested(MetadataGenerationEnvironment environment) {
		return environment.getNestedConfigurationPropertyAnnotation(this.field) != null
				|| environment.getNestedConfigurationPropertyAnnotation(getGetter()) != null;
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
		boolean isCollection = env.getTypeUtils().isCollectionOrMap(getType());
		boolean hasGetter = getGetter() != null;
		boolean hasSetter = getSetter() != null;
		return !env.isExcluded(getType()) && hasGetter && (hasSetter || isCollection);
	}

}
