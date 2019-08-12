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

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

/**
 * A {@link PropertyDescriptor} for a standard JavaBean property.
 *
 * @author Stephane Nicoll
 */
class JavaBeanPropertyDescriptor extends PropertyDescriptor<ExecutableElement> {

	JavaBeanPropertyDescriptor(TypeElement ownerElement, ExecutableElement factoryMethod, ExecutableElement getter,
			String name, TypeMirror type, VariableElement field, ExecutableElement setter) {
		super(ownerElement, factoryMethod, getter, name, type, field, getter, setter);
	}

	@Override
	protected boolean isProperty(MetadataGenerationEnvironment env) {
		boolean isCollection = env.getTypeUtils().isCollectionOrMap(getType());
		return !env.isExcluded(getType()) && getGetter() != null && (getSetter() != null || isCollection);
	}

	@Override
	protected Object resolveDefaultValue(MetadataGenerationEnvironment environment) {
		return environment.getFieldDefaultValue(getOwnerElement(), getName());
	}

}
