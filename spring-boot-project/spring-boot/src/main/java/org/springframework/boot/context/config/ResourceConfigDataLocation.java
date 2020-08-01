/*
 * Copyright 2012-2020 the original author or authors.
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

import java.io.IOException;
import java.util.List;

import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.FileUrlResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * {@link ConfigDataLocation} backed by a {@link Resource}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class ResourceConfigDataLocation extends ConfigDataLocation {

	private final String name;

	private final Resource resource;

	private final PropertySourceLoader propertySourceLoader;

	/**
	 * Create a new {@link ResourceConfigDataLocation} instance.
	 * @param name the source location
	 * @param resource the underlying resource
	 * @param propertySourceLoader the loader that should be used to load the resource
	 */
	ResourceConfigDataLocation(String name, Resource resource, PropertySourceLoader propertySourceLoader) {
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(resource, "Resource must not be null");
		Assert.notNull(propertySourceLoader, "PropertySourceLoader must not be null");
		this.name = name;
		this.resource = resource;
		this.propertySourceLoader = propertySourceLoader;
	}

	String getLocation() {
		return this.name;
	}

	List<PropertySource<?>> load() throws IOException {
		return this.propertySourceLoader.load(this.name, this.resource);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		ResourceConfigDataLocation other = (ResourceConfigDataLocation) obj;
		return this.resource.equals(other.resource);
	}

	@Override
	public int hashCode() {
		return this.resource.hashCode();
	}

	@Override
	public String toString() {
		if (this.resource instanceof FileSystemResource || this.resource instanceof FileUrlResource) {
			try {
				return "file [" + this.resource.getFile().toString() + "]";
			}
			catch (IOException ex) {
			}
		}
		return this.resource.toString();
	}

}
