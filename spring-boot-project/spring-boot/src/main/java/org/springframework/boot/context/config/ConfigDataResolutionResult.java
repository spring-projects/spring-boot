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

/**
 * Result returned from {@link ConfigDataLocationResolvers} containing both the
 * {@link ConfigDataResource} and the original {@link ConfigDataLocation}.
 *
 * @author Phillip Webb
 */
class ConfigDataResolutionResult {

	private final ConfigDataLocation location;

	private final ConfigDataResource resource;

	private final boolean profileSpecific;

	/**
     * Constructs a new ConfigDataResolutionResult with the specified location, resource, and profileSpecific flag.
     * 
     * @param location the location of the configuration data
     * @param resource the resource containing the configuration data
     * @param profileSpecific indicates whether the configuration data is profile-specific
     */
    ConfigDataResolutionResult(ConfigDataLocation location, ConfigDataResource resource, boolean profileSpecific) {
		this.location = location;
		this.resource = resource;
		this.profileSpecific = profileSpecific;
	}

	/**
     * Returns the location of the configuration data.
     *
     * @return the location of the configuration data
     */
    ConfigDataLocation getLocation() {
		return this.location;
	}

	/**
     * Returns the resource associated with this ConfigDataResolutionResult.
     *
     * @return the resource associated with this ConfigDataResolutionResult
     */
    ConfigDataResource getResource() {
		return this.resource;
	}

	/**
     * Returns a boolean value indicating whether the configuration data resolution result is profile specific.
     * 
     * @return true if the configuration data resolution result is profile specific, false otherwise
     */
    boolean isProfileSpecific() {
		return this.profileSpecific;
	}

}
