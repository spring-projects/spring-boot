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

package org.springframework.boot.actuate.autoconfigure.metrics.export.humio;

import java.util.Map;

import io.micrometer.humio.HumioConfig;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryPropertiesConfigAdapter;

/**
 * Adapter to convert {@link HumioProperties} to a {@link HumioConfig}.
 *
 * @author Andy Wilkinson
 */
class HumioPropertiesConfigAdapter extends StepRegistryPropertiesConfigAdapter<HumioProperties> implements HumioConfig {

	/**
     * Constructs a new HumioPropertiesConfigAdapter with the specified HumioProperties.
     *
     * @param properties the HumioProperties to be used for configuring the adapter
     */
    HumioPropertiesConfigAdapter(HumioProperties properties) {
		super(properties);
	}

	/**
     * Returns the prefix for the Humio metrics export configuration properties.
     * The prefix is used to group the properties under a common namespace.
     * 
     * @return the prefix for the Humio metrics export configuration properties
     */
    @Override
	public String prefix() {
		return "management.humio.metrics.export";
	}

	/**
     * Retrieves the value associated with the specified key from the HumioPropertiesConfigAdapter.
     * 
     * @param k the key whose associated value is to be retrieved
     * @return the value to which the specified key is mapped, or null if the key is not found
     */
    @Override
	public String get(String k) {
		return null;
	}

	/**
     * Returns the URI for the Humio properties.
     * 
     * @return the URI for the Humio properties
     */
    @Override
	public String uri() {
		return get(HumioProperties::getUri, HumioConfig.super::uri);
	}

	/**
     * Returns a map of tags.
     *
     * This method overrides the tags() method in the HumioConfig interface.
     * It calls the get() method from the HumioProperties class to retrieve the tags,
     * and if the tags are not available, it falls back to the tags() method in the HumioConfig interface.
     *
     * @return a map of tags
     */
    @Override
	public Map<String, String> tags() {
		return get(HumioProperties::getTags, HumioConfig.super::tags);
	}

	/**
     * Returns the API token for accessing the Humio API.
     * 
     * @return the API token
     */
    @Override
	public String apiToken() {
		return get(HumioProperties::getApiToken, HumioConfig.super::apiToken);
	}

}
