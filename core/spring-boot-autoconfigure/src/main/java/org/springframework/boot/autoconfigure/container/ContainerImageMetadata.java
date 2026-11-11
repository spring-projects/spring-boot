/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.container;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.AttributeAccessor;

/**
 * Metadata about a container image that can be added to an {@link AttributeAccessor}.
 * Primarily designed to be attached to {@link BeanDefinition BeanDefinitions} created in
 * support of Testcontainers or Docker Compose.
 *
 * @param imageName the container image name or {@code null} if the image name is not yet
 * known
 * @author Phillip Webb
 * @since 3.4.0
 */
public record ContainerImageMetadata(@Nullable String imageName) {

	static final String NAME = ContainerImageMetadata.class.getName();

	/**
	 * Add this container image metadata to the given attributes.
	 * @param attributes the attributes to add the metadata to
	 */
	public void addTo(@Nullable AttributeAccessor attributes) {
		if (attributes != null) {
			attributes.setAttribute(NAME, this);
		}
	}

	/**
	 * Return {@code true} if {@link ContainerImageMetadata} has been added to the given
	 * attributes.
	 * @param attributes the attributes to check
	 * @return if metadata is present
	 */
	public static boolean isPresent(@Nullable AttributeAccessor attributes) {
		return getFrom(attributes) != null;
	}

	/**
	 * Return {@link ContainerImageMetadata} from the given attributes or {@code null} if
	 * no metadata has been added.
	 * @param attributes the attributes
	 * @return the metadata or {@code null}
	 */
	public static @Nullable ContainerImageMetadata getFrom(@Nullable AttributeAccessor attributes) {
		return (attributes != null) ? (ContainerImageMetadata) attributes.getAttribute(NAME) : null;
	}

}
