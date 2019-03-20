/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.context.config;

import org.springframework.core.io.Resource;

/**
 * Exception thrown when a {@link Resource} defined by a property is not found.
 *
 * @author Stephane Nicoll
 * @since 1.5.0
 */
@SuppressWarnings("serial")
public class ResourceNotFoundException extends RuntimeException {

	private final String propertyName;

	private final Resource resource;

	public ResourceNotFoundException(String propertyName, Resource resource) {
		super(String.format("%s defined by '%s' does not exist", resource, propertyName));
		this.propertyName = propertyName;
		this.resource = resource;
	}

	/**
	 * Return the name of the property that defines the resource.
	 * @return the property
	 */
	public String getPropertyName() {
		return this.propertyName;
	}

	/**
	 * Return the {@link Resource}.
	 * @return the resource
	 */
	public Resource getResource() {
		return this.resource;
	}

}
