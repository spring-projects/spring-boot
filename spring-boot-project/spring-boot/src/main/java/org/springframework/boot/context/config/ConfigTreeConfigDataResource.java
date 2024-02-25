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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import org.springframework.boot.env.ConfigTreePropertySource;
import org.springframework.util.Assert;

/**
 * {@link ConfigDataResource} backed by a config tree directory.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @since 2.4.0
 * @see ConfigTreePropertySource
 */
public class ConfigTreeConfigDataResource extends ConfigDataResource {

	private final Path path;

	/**
     * Constructs a new ConfigTreeConfigDataResource with the specified path.
     * 
     * @param path the path to the configuration data resource (must not be null)
     * @throws IllegalArgumentException if the path is null
     */
    ConfigTreeConfigDataResource(String path) {
		Assert.notNull(path, "Path must not be null");
		this.path = Paths.get(path).toAbsolutePath();
	}

	/**
     * Constructs a new ConfigTreeConfigDataResource with the specified path.
     * 
     * @param path the path to the config data resource
     * @throws IllegalArgumentException if the path is null
     */
    ConfigTreeConfigDataResource(Path path) {
		Assert.notNull(path, "Path must not be null");
		this.path = path.toAbsolutePath();
	}

	/**
     * Returns the path of the ConfigTreeConfigDataResource.
     *
     * @return the path of the ConfigTreeConfigDataResource
     */
    Path getPath() {
		return this.path;
	}

	/**
     * Compares this ConfigTreeConfigDataResource object to the specified object for equality.
     * 
     * @param obj the object to compare to
     * @return true if the objects are equal, false otherwise
     */
    @Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		ConfigTreeConfigDataResource other = (ConfigTreeConfigDataResource) obj;
		return Objects.equals(this.path, other.path);
	}

	/**
     * Returns the hash code value for this ConfigTreeConfigDataResource object.
     * The hash code is generated based on the path of the resource.
     *
     * @return the hash code value for this ConfigTreeConfigDataResource object
     */
    @Override
	public int hashCode() {
		return this.path.hashCode();
	}

	/**
     * Returns a string representation of the ConfigTreeConfigDataResource object.
     * 
     * @return a string representation of the object in the format "config tree [path]"
     */
    @Override
	public String toString() {
		return "config tree [" + this.path + "]";
	}

}
