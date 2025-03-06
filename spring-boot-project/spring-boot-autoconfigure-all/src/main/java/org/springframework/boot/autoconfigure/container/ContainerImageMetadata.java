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

package org.springframework.boot.autoconfigure.container;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.AttributeAccessor;

/**
 * Metadata about a container image that can be added to an {@link AttributeAccessor}.
 * Primarily designed to be attached to {@link BeanDefinition BeanDefinitions} created in
 * support of Testcontainers or Docker Compose.
 *
 * @param imageName the contaimer image name or {@code null} if the image name is not yet
 * known
 * @author Phillip Webb
 * @since 3.4.0
 */
public record ContainerImageMetadata(String imageName) {

	static final String NAME = ContainerImageMetadata.class.getName();

	/**
	 * Add this container image metadata to the given attributes.
	 * @param attributes the attributes to add the metadata to
	 */
	public void addTo(AttributeAccessor attributes) {
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
	public static boolean isPresent(AttributeAccessor attributes) {
		return getFrom(attributes) != null;
	}

	/**
	 * Return {@link ContainerImageMetadata} from the given attributes or {@code null} if
	 * no metadata has been added.
	 * @param attributes the attributes
	 * @return the metadata or {@code null}
	 */
	public static ContainerImageMetadata getFrom(AttributeAccessor attributes) {
		return (attributes != null) ? (ContainerImageMetadata) attributes.getAttribute(NAME) : null;
	}

}
