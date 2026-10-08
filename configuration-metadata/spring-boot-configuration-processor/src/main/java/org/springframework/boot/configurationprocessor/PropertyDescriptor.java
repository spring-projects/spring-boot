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

import java.util.Arrays;

import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationprocessor.metadata.ItemDeprecation;
import org.springframework.boot.configurationprocessor.metadata.ItemHint;
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata;
import org.springframework.boot.configurationprocessor.model.Declaration;
import org.springframework.boot.configurationprocessor.model.MethodDeclaration;
import org.springframework.boot.configurationprocessor.model.TypeDeclaration;
import org.springframework.boot.configurationprocessor.model.TypeReference;

/**
 * Description of a property that can be candidate for metadata generation.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
abstract class PropertyDescriptor {

	private final String name;

	private final TypeReference type;

	private final TypeDeclaration declaringElement;

	private final MethodDeclaration getter;

	/**
	 * Create a new {@link PropertyDescriptor} instance.
	 * @param name the property name
	 * @param type the property type
	 * @param declaringElement the element that declared the item
	 * @param getter the getter for the property or {@code null}
	 */
	PropertyDescriptor(String name, TypeReference type, TypeDeclaration declaringElement, MethodDeclaration getter) {
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
	TypeReference getType() {
		return this.type;
	}

	/**
	 * Return the element that declared the property.
	 * @return the declaring element
	 */
	protected final TypeDeclaration getDeclaringElement() {
		return this.declaringElement;
	}

	/**
	 * Return the getter for the property.
	 * @return the getter or {@code null}
	 */
	protected final MethodDeclaration getGetter() {
		return this.getter;
	}

	/**
	 * Return the {@link Declaration} that primarily defines the property.
	 * @return the source element
	 */
	protected abstract Declaration getSourceElement();

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
	 * Resolve the {@link ItemHint} for this property.
	 * @param prefix the property prefix
	 * @param environment the metadata generation environment
	 * @return the item hint or {@code null}
	 */
	protected ItemHint resolveItemHint(String prefix, MetadataGenerationEnvironment environment) {
		return null;
	}

	/**
	 * Return if this is a nested property.
	 * @param environment the metadata generation environment
	 * @return if the property is nested
	 * @see #isMarkedAsNested(MetadataGenerationEnvironment)
	 */
	boolean isNested(MetadataGenerationEnvironment environment) {
		TypeDeclaration typeElement = getType().getDeclaration();
		if (typeElement == null || typeElement.isEnum()
				|| environment.getConfigurationPropertiesAnnotation(getGetter()) != null) {
			return false;
		}
		if (isMarkedAsNested(environment)) {
			return true;
		}
		return !isCyclePresent(typeElement, getDeclaringElement())
				&& isParentTheSame(typeElement, getDeclaringElement());
	}

	/**
	 * Return if this property has been explicitly marked as nested (for example using an
	 * annotation}.
	 * @param environment the metadata generation environment
	 * @return if the property has been marked as nested
	 */
	protected abstract boolean isMarkedAsNested(MetadataGenerationEnvironment environment);

	private boolean isCyclePresent(TypeDeclaration returnType, TypeDeclaration element) {
		if (element.getEnclosingType() == null) {
			return false;
		}
		if (element.getEnclosingType().equals(returnType)) {
			return true;
		}
		return isCyclePresent(returnType, element.getEnclosingType());
	}

	private boolean isParentTheSame(TypeDeclaration returnType, TypeDeclaration element) {
		if (returnType == null || element == null) {
			return false;
		}
		returnType = getTopLevelType(returnType);
		TypeDeclaration candidate = element;
		while (candidate != null) {
			if (returnType.equals(getTopLevelType(candidate))) {
				return true;
			}
			candidate = candidate.getSuperclass();
		}
		return false;
	}

	private TypeDeclaration getTopLevelType(TypeDeclaration element) {
		if (element.getEnclosingType() == null) {
			return element;
		}
		return getTopLevelType(element.getEnclosingType());
	}

	private ItemMetadata resolveItemMetadataGroup(String prefix, MetadataGenerationEnvironment environment) {
		TypeDeclaration propertyElement = getType().getDeclaration();
		String nestedPrefix = ConfigurationMetadata.nestedPrefix(prefix, getName());
		String dataType = propertyElement.getQualifiedName();
		String ownerType = getDeclaringElement().getQualifiedName();
		String sourceMethod = (getGetter() != null) ? getGetter().getSignature() : null;
		return ItemMetadata.newGroup(nestedPrefix, dataType, ownerType, sourceMethod);
	}

	private ItemMetadata resolveItemMetadataProperty(String prefix, MetadataGenerationEnvironment environment) {
		String dataType = resolveType();
		String ownerType = getDeclaringElement().getQualifiedName();
		String description = resolveDescription(environment);
		Object defaultValue = resolveDefaultValue(environment);
		ItemDeprecation deprecation = resolveItemDeprecation(environment);
		return ItemMetadata.newProperty(prefix, getName(), dataType, ownerType, null, description, defaultValue,
				deprecation);
	}

	protected final ItemDeprecation resolveItemDeprecation(MetadataGenerationEnvironment environment,
			Declaration... elements) {
		boolean deprecated = Arrays.stream(elements).anyMatch(environment::isDeprecated);
		return deprecated ? environment.resolveItemDeprecation(getGetter()) : null;
	}

	private String resolveType() {
		return (getType() != null) ? getType().getName(getDeclaringElement()) : null;
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
	 * Resolve the {@link ItemDeprecation} for this property.
	 * @param environment the metadata generation environment
	 * @return the deprecation or {@code null}
	 */
	protected abstract ItemDeprecation resolveItemDeprecation(MetadataGenerationEnvironment environment);

	/**
	 * Return true if this descriptor is for a property.
	 * @param environment the metadata generation environment
	 * @return if this is a property
	 */
	abstract boolean isProperty(MetadataGenerationEnvironment environment);

}
