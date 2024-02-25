/*
 * Copyright 2012-2021 the original author or authors.
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

import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.util.StringUtils;

/**
 * A reference expanded from the original {@link ConfigDataLocation} that can ultimately
 * be resolved to one or more {@link StandardConfigDataResource resources}.
 *
 * @author Phillip Webb
 */
class StandardConfigDataReference {

	private final ConfigDataLocation configDataLocation;

	private final String resourceLocation;

	private final String directory;

	private final String profile;

	private final PropertySourceLoader propertySourceLoader;

	/**
	 * Create a new {@link StandardConfigDataReference} instance.
	 * @param configDataLocation the original location passed to the resolver
	 * @param directory the directory of the resource or {@code null} if the reference is
	 * to a file
	 * @param root the root of the resource location
	 * @param profile the profile being loaded
	 * @param extension the file extension for the resource
	 * @param propertySourceLoader the property source loader that should be used for this
	 * reference
	 */
	StandardConfigDataReference(ConfigDataLocation configDataLocation, String directory, String root, String profile,
			String extension, PropertySourceLoader propertySourceLoader) {
		this.configDataLocation = configDataLocation;
		String profileSuffix = (StringUtils.hasText(profile)) ? "-" + profile : "";
		this.resourceLocation = root + profileSuffix + ((extension != null) ? "." + extension : "");
		this.directory = directory;
		this.profile = profile;
		this.propertySourceLoader = propertySourceLoader;
	}

	/**
     * Returns the location of the configuration data.
     *
     * @return the location of the configuration data
     */
    ConfigDataLocation getConfigDataLocation() {
		return this.configDataLocation;
	}

	/**
     * Returns the resource location of the StandardConfigDataReference.
     * 
     * @return the resource location of the StandardConfigDataReference
     */
    String getResourceLocation() {
		return this.resourceLocation;
	}

	/**
     * Checks if the directory is mandatory.
     * 
     * @return true if the directory is mandatory, false otherwise
     */
    boolean isMandatoryDirectory() {
		return !this.configDataLocation.isOptional() && this.directory != null;
	}

	/**
     * Returns the directory path associated with this StandardConfigDataReference object.
     *
     * @return the directory path
     */
    String getDirectory() {
		return this.directory;
	}

	/**
     * Returns the profile of the StandardConfigDataReference.
     *
     * @return the profile of the StandardConfigDataReference
     */
    String getProfile() {
		return this.profile;
	}

	/**
     * Checks if the configuration data reference is skippable.
     * 
     * @return true if the configuration data location is optional, or if the directory or profile is not null; false otherwise.
     */
    boolean isSkippable() {
		return this.configDataLocation.isOptional() || this.directory != null || this.profile != null;
	}

	/**
     * Returns the property source loader used by this StandardConfigDataReference.
     *
     * @return the property source loader
     */
    PropertySourceLoader getPropertySourceLoader() {
		return this.propertySourceLoader;
	}

	/**
     * Compares this StandardConfigDataReference object to the specified object for equality.
     * Returns true if the specified object is also a StandardConfigDataReference and their resourceLocation properties are equal.
     * 
     * @param obj the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if ((obj == null) || (getClass() != obj.getClass())) {
			return false;
		}
		StandardConfigDataReference other = (StandardConfigDataReference) obj;
		return this.resourceLocation.equals(other.resourceLocation);
	}

	/**
     * Returns the hash code value for this StandardConfigDataReference object.
     * 
     * @return the hash code value for this object
     */
    @Override
	public int hashCode() {
		return this.resourceLocation.hashCode();
	}

	/**
     * Returns the string representation of the resource location.
     *
     * @return the resource location as a string
     */
    @Override
	public String toString() {
		return this.resourceLocation;
	}

}
