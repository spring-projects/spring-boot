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

	/**
	 * Constructs a new JavaBeanPropertyDescriptor with the specified parameters.
	 * @param ownerElement the TypeElement representing the owner class of the property
	 * @param factoryMethod the ExecutableElement representing the factory method used to
	 * create instances of the owner class
	 * @param getter the ExecutableElement representing the getter method for the property
	 * @param name the name of the property
	 * @param type the TypeMirror representing the type of the property
	 * @param field the VariableElement representing the field associated with the
	 * property
	 * @param setter the ExecutableElement representing the setter method for the property
	 */
	JavaBeanPropertyDescriptor(TypeElement ownerElement, ExecutableElement factoryMethod, ExecutableElement getter,
			String name, TypeMirror type, VariableElement field, ExecutableElement setter) {
		super(ownerElement, factoryMethod, getter, name, type, field, getter, setter);
	}

	/**
	 * Determines if the property represented by this JavaBeanPropertyDescriptor is a
	 * valid property.
	 * @param env the MetadataGenerationEnvironment used for metadata generation
	 * @return true if the property is valid, false otherwise
	 */
	@Override
	protected boolean isProperty(MetadataGenerationEnvironment env) {
		boolean isCollection = env.getTypeUtils().isCollectionOrMap(getType());
		return !env.isExcluded(getType()) && getGetter() != null && (getSetter() != null || isCollection);
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

}
