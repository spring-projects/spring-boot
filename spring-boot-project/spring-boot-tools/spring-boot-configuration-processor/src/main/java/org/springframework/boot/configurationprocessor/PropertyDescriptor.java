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

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationprocessor.metadata.ItemDeprecation;
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata;

/**
 * Description of a property that can be candidate for metadata generation.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
abstract class PropertyDescriptor {

	private final String name;

	private final TypeMirror type;

	private final TypeElement declaringElement;

	private final ExecutableElement getter;

	/**
	 * Create a new {@link PropertyDescriptor} instance.
	 * @param name the property name
	 * @param type the property type
	 * @param declaringElement the element that declared the item
	 * @param getter the getter for the property or {@code null}
	 */
	PropertyDescriptor(String name, TypeMirror type, TypeElement declaringElement, ExecutableElement getter) {
		this.declaringElement = declaringElement;
		this.name = name;
		this.type = type;
		this.getter = getter;
	}

	/**
	 * Return the name of the property.
	 * @return the property name
	 */
	String getName() {
		return this.name;
	}

	/**
	 * Return the type of the property.
	 * @return the property type
	 */
	TypeMirror getType() {
		return this.type;
	}

	/**
	 * Return the element that declared the property.
	 * @return the declaring element
	 */
	protected final TypeElement getDeclaringElement() {
		return this.declaringElement;
	}

	/**
	 * Return the getter for the property.
	 * @return the getter or {@code null}
	 */
	protected final ExecutableElement getGetter() {
		return this.getter;
	}

	/**
	 * Resolve the {@link ItemMetadata} for this property.
	 * @param prefix the property prefix
	 * @param environment the metadata generation environment
	 * @return the item metadata or {@code null}
	 */
	final ItemMetadata resolveItemMetadata(String prefix, MetadataGenerationEnvironment environment) {
		if (isNested(environment)) {
			return resolveItemMetadataGroup(prefix, environment);
		}
		if (isProperty(environment)) {
			return resolveItemMetadataProperty(prefix, environment);
		}
		return null;
	}

	/**
	 * Return if this is a nested property.
	 * @param environment the metadata generation environment
	 * @return if the property is nested
	 * @see #isMarkedAsNested(MetadataGenerationEnvironment)
	 */
	boolean isNested(MetadataGenerationEnvironment environment) {
		Element typeElement = environment.getTypeUtils().asElement(getType());
		if (!(typeElement instanceof TypeElement) || typeElement.getKind() == ElementKind.ENUM
				|| environment.getConfigurationPropertiesAnnotation(getGetter()) != null) {
			return false;
		}
		if (isMarkedAsNested(environment)) {
			return true;
		}
		return !isCyclePresent(typeElement, getDeclaringElement())
				&& isParentTheSame(environment, typeElement, getDeclaringElement());
	}

	/**
	 * Return if this property has been explicitly marked as nested (for example using an
	 * annotation}.
	 * @param environment the metadata generation environment
	 * @return if the property has been marked as nested
	 */
	protected abstract boolean isMarkedAsNested(MetadataGenerationEnvironment environment);

	private boolean isCyclePresent(Element returnType, Element element) {
		if (!(element.getEnclosingElement() instanceof TypeElement)) {
			return false;
		}
		if (element.getEnclosingElement().equals(returnType)) {
			return true;
		}
		return isCyclePresent(returnType, element.getEnclosingElement());
	}

	private boolean isParentTheSame(MetadataGenerationEnvironment environment, Element returnType,
			TypeElement element) {
		if (returnType == null || element == null) {
			return false;
		}
		returnType = getTopLevelType(returnType);
		Element candidate = element;
		while (candidate instanceof TypeElement) {
			if (returnType.equals(getTopLevelType(candidate))) {
				return true;
			}
			candidate = environment.getTypeUtils().asElement(((TypeElement) candidate).getSuperclass());
		}
		return false;
	}

	private Element getTopLevelType(Element element) {
		if (!(element.getEnclosingElement() instanceof TypeElement)) {
			return element;
		}
		return getTopLevelType(element.getEnclosingElement());
	}

	private ItemMetadata resolveItemMetadataGroup(String prefix, MetadataGenerationEnvironment environment) {
		Element propertyElement = environment.getTypeUtils().asElement(getType());
		String nestedPrefix = ConfigurationMetadata.nestedPrefix(prefix, getName());
		String dataType = environment.getTypeUtils().getQualifiedName(propertyElement);
		String ownerType = environment.getTypeUtils().getQualifiedName(getDeclaringElement());
		String sourceMethod = (getGetter() != null) ? getGetter().toString() : null;
		return ItemMetadata.newGroup(nestedPrefix, dataType, ownerType, sourceMethod);
	}

	private ItemMetadata resolveItemMetadataProperty(String prefix, MetadataGenerationEnvironment environment) {
		String dataType = resolveType(environment);
		String ownerType = environment.getTypeUtils().getQualifiedName(getDeclaringElement());
		String description = resolveDescription(environment);
		Object defaultValue = resolveDefaultValue(environment);
		ItemDeprecation deprecation = resolveItemDeprecation(environment);
		return ItemMetadata.newProperty(prefix, getName(), dataType, ownerType, null, description, defaultValue,
				deprecation);
	}

	private String resolveType(MetadataGenerationEnvironment environment) {
		return environment.getTypeUtils().getType(getDeclaringElement(), getType());
	}

	private ItemDeprecation resolveItemDeprecation(MetadataGenerationEnvironment environment) {
		boolean deprecated = getDeprecatableElements().stream().anyMatch(environment::isDeprecated);
		return deprecated ? environment.resolveItemDeprecation(getGetter()) : null;
	}

	/**
	 * Resolve the property description.
	 * @param environment the metadata generation environment
	 * @return the property description
	 */
	protected abstract String resolveDescription(MetadataGenerationEnvironment environment);

	/**
	 * Resolve the default value for this property.
	 * @param environment the metadata generation environment
	 * @return the default value or {@code null}
	 */
	protected abstract Object resolveDefaultValue(MetadataGenerationEnvironment environment);

	/**
	 * Return all the elements that should be considered when checking for deprecation
	 * annotations.
	 * @return the deprecatable elements
	 */
	protected abstract List<Element> getDeprecatableElements();

	/**
	 * Return true if this descriptor is for a property.
	 * @param environment the metadata generation environment
	 * @return if this is a property
	 */
	abstract boolean isProperty(MetadataGenerationEnvironment environment);

}
