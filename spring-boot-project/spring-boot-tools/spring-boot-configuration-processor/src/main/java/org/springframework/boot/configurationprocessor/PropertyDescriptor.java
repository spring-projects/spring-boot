/*
 * Copyright 2012-2022 the original author or authors.
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

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationprocessor.metadata.ItemDeprecation;
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata;

/**
 * Description of a property that can be candidate for metadata generation.
 *
 * @param <S> the type of the source element that determines the property
 * @author Stephane Nicoll
 */
abstract class PropertyDescriptor<S extends Element> {

	private final TypeElement ownerElement;

	private final ExecutableElement factoryMethod;

	private final S source;

	private final String name;

	private final TypeMirror type;

	private final VariableElement field;

	private final ExecutableElement getter;

	private final ExecutableElement setter;

	/**
	 * Constructs a new PropertyDescriptor with the specified parameters.
	 * @param ownerElement the TypeElement representing the owner of the property
	 * @param factoryMethod the ExecutableElement representing the factory method used to
	 * create the property
	 * @param source the source object from which the property is derived
	 * @param name the name of the property
	 * @param type the TypeMirror representing the type of the property
	 * @param field the VariableElement representing the field associated with the
	 * property
	 * @param getter the ExecutableElement representing the getter method for the property
	 * @param setter the ExecutableElement representing the setter method for the property
	 */
	protected PropertyDescriptor(TypeElement ownerElement, ExecutableElement factoryMethod, S source, String name,
			TypeMirror type, VariableElement field, ExecutableElement getter, ExecutableElement setter) {
		this.ownerElement = ownerElement;
		this.factoryMethod = factoryMethod;
		this.source = source;
		this.name = name;
		this.type = type;
		this.field = field;
		this.getter = getter;
		this.setter = setter;
	}

	/**
	 * Returns the owner element of this PropertyDescriptor.
	 * @return the owner element of this PropertyDescriptor
	 */
	TypeElement getOwnerElement() {
		return this.ownerElement;
	}

	/**
	 * Returns the factory method associated with this PropertyDescriptor.
	 * @return the factory method associated with this PropertyDescriptor
	 */
	ExecutableElement getFactoryMethod() {
		return this.factoryMethod;
	}

	/**
	 * Returns the source of this PropertyDescriptor.
	 * @return the source of this PropertyDescriptor
	 */
	S getSource() {
		return this.source;
	}

	/**
	 * Returns the name of the property.
	 * @return the name of the property
	 */
	String getName() {
		return this.name;
	}

	/**
	 * Returns the TypeMirror object representing the type of this PropertyDescriptor.
	 * @return the TypeMirror object representing the type of this PropertyDescriptor
	 */
	TypeMirror getType() {
		return this.type;
	}

	/**
	 * Returns the field associated with this PropertyDescriptor.
	 * @return the field associated with this PropertyDescriptor
	 */
	VariableElement getField() {
		return this.field;
	}

	/**
	 * Returns the getter method associated with this PropertyDescriptor.
	 * @return the getter method associated with this PropertyDescriptor
	 */
	ExecutableElement getGetter() {
		return this.getter;
	}

	/**
	 * Returns the executable element representing the setter method of this
	 * PropertyDescriptor.
	 * @return the executable element representing the setter method of this
	 * PropertyDescriptor
	 */
	ExecutableElement getSetter() {
		return this.setter;
	}

	/**
	 * Checks if the given property is valid in the specified metadata generation
	 * environment.
	 * @param environment the metadata generation environment to check against
	 * @return {@code true} if the property is valid in the specified environment,
	 * {@code false} otherwise
	 */
	protected abstract boolean isProperty(MetadataGenerationEnvironment environment);

	/**
	 * Resolves the default value for this property.
	 * @param environment the metadata generation environment
	 * @return the resolved default value
	 */
	protected abstract Object resolveDefaultValue(MetadataGenerationEnvironment environment);

	/**
	 * Resolves the deprecation status of an item in the given metadata generation
	 * environment.
	 * @param environment the metadata generation environment
	 * @return the item deprecation if the item is deprecated, otherwise null
	 */
	protected ItemDeprecation resolveItemDeprecation(MetadataGenerationEnvironment environment) {
		boolean deprecated = environment.isDeprecated(getGetter()) || environment.isDeprecated(getSetter())
				|| environment.isDeprecated(getField()) || environment.isDeprecated(getFactoryMethod());
		return deprecated ? environment.resolveItemDeprecation(getGetter()) : null;
	}

	/**
	 * Checks if the property is nested.
	 * @param environment the metadata generation environment
	 * @return true if the property is nested, false otherwise
	 */
	protected boolean isNested(MetadataGenerationEnvironment environment) {
		Element typeElement = environment.getTypeUtils().asElement(getType());
		if (!(typeElement instanceof TypeElement) || typeElement.getKind() == ElementKind.ENUM) {
			return false;
		}
		if (environment.getConfigurationPropertiesAnnotation(getGetter()) != null) {
			return false;
		}
		if (environment.getNestedConfigurationPropertyAnnotation(getField()) != null) {
			return true;
		}
		if (isCyclePresent(typeElement, getOwnerElement())) {
			return false;
		}
		return isParentTheSame(environment, typeElement, getOwnerElement());
	}

	/**
	 * Resolves the item metadata for the given prefix and environment.
	 * @param prefix the prefix to be resolved
	 * @param environment the metadata generation environment
	 * @return the resolved item metadata, or null if not found
	 */
	ItemMetadata resolveItemMetadata(String prefix, MetadataGenerationEnvironment environment) {
		if (isNested(environment)) {
			return resolveItemMetadataGroup(prefix, environment);
		}
		else if (isProperty(environment)) {
			return resolveItemMetadataProperty(prefix, environment);
		}
		return null;
	}

	/**
	 * Resolves the item metadata property.
	 * @param prefix the prefix of the property
	 * @param environment the metadata generation environment
	 * @return the resolved item metadata property
	 */
	private ItemMetadata resolveItemMetadataProperty(String prefix, MetadataGenerationEnvironment environment) {
		String dataType = resolveType(environment);
		String ownerType = environment.getTypeUtils().getQualifiedName(getOwnerElement());
		String description = resolveDescription(environment);
		Object defaultValue = resolveDefaultValue(environment);
		ItemDeprecation deprecation = resolveItemDeprecation(environment);
		return ItemMetadata.newProperty(prefix, getName(), dataType, ownerType, null, description, defaultValue,
				deprecation);
	}

	/**
	 * Resolves the item metadata group for the given prefix and environment.
	 * @param prefix the prefix to be used for nested properties
	 * @param environment the metadata generation environment
	 * @return the resolved item metadata group
	 */
	private ItemMetadata resolveItemMetadataGroup(String prefix, MetadataGenerationEnvironment environment) {
		Element propertyElement = environment.getTypeUtils().asElement(getType());
		String nestedPrefix = ConfigurationMetadata.nestedPrefix(prefix, getName());
		String dataType = environment.getTypeUtils().getQualifiedName(propertyElement);
		String ownerType = environment.getTypeUtils().getQualifiedName(getOwnerElement());
		String sourceMethod = (getGetter() != null) ? getGetter().toString() : null;
		return ItemMetadata.newGroup(nestedPrefix, dataType, ownerType, sourceMethod);
	}

	/**
	 * Resolves the type of the property.
	 * @param environment the metadata generation environment
	 * @return the resolved type of the property
	 */
	private String resolveType(MetadataGenerationEnvironment environment) {
		return environment.getTypeUtils().getType(getOwnerElement(), getType());
	}

	/**
	 * Resolves the description of the property using the provided metadata generation
	 * environment.
	 * @param environment the metadata generation environment
	 * @return the description of the property
	 */
	private String resolveDescription(MetadataGenerationEnvironment environment) {
		return environment.getTypeUtils().getJavaDoc(getField());
	}

	/**
	 * Checks if there is a cycle present in the inheritance hierarchy between the given
	 * return type and the enclosing element.
	 * @param returnType the return type to check for cycle
	 * @param element the element to check for cycle
	 * @return true if a cycle is present, false otherwise
	 */
	private boolean isCyclePresent(Element returnType, Element element) {
		if (!(element.getEnclosingElement() instanceof TypeElement)) {
			return false;
		}
		if (element.getEnclosingElement().equals(returnType)) {
			return true;
		}
		return isCyclePresent(returnType, element.getEnclosingElement());
	}

	/**
	 * Checks if the parent of the given element is the same as the return type.
	 * @param environment the metadata generation environment
	 * @param returnType the return type element
	 * @param element the element to check
	 * @return true if the parent of the element is the same as the return type, false
	 * otherwise
	 */
	private boolean isParentTheSame(MetadataGenerationEnvironment environment, Element returnType,
			TypeElement element) {
		if (returnType == null || element == null) {
			return false;
		}
		returnType = getTopLevelType(returnType);
		Element candidate = element;
		while (candidate != null && candidate instanceof TypeElement) {
			if (returnType.equals(getTopLevelType(candidate))) {
				return true;
			}
			candidate = environment.getTypeUtils().asElement(((TypeElement) candidate).getSuperclass());
		}
		return false;
	}

	/**
	 * Returns the top level type of the given element. If the enclosing element of the
	 * given element is not an instance of TypeElement, then the given element itself is
	 * considered as the top level type. Otherwise, the enclosing element is recursively
	 * checked until a top level type is found.
	 * @param element the element for which the top level type needs to be determined
	 * @return the top level type of the given element
	 */
	private Element getTopLevelType(Element element) {
		if (!(element.getEnclosingElement() instanceof TypeElement)) {
			return element;
		}
		return getTopLevelType(element.getEnclosingElement());
	}

}
