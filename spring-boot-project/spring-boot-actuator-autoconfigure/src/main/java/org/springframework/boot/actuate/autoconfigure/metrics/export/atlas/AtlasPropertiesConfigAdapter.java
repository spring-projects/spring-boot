/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.export.atlas;

import java.time.Duration;

import com.netflix.spectator.atlas.AtlasConfig;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.PropertiesConfigAdapter;

/**
 * Adapter to convert {@link AtlasProperties} to an {@link AtlasConfig}.
 *
 * @author Jon Schneider
 * @author Phillip Webb
 */
class AtlasPropertiesConfigAdapter extends PropertiesConfigAdapter<AtlasProperties> implements AtlasConfig {

	/**
     * Constructs a new AtlasPropertiesConfigAdapter with the specified AtlasProperties.
     * 
     * @param properties the AtlasProperties to be used by the adapter
     */
    AtlasPropertiesConfigAdapter(AtlasProperties properties) {
		super(properties);
	}

	/**
     * Retrieves the value associated with the specified key from the AtlasPropertiesConfigAdapter.
     *
     * @param key the key whose associated value is to be retrieved
     * @return the value to which the specified key is mapped, or null if the key is not found
     */
    @Override
	public String get(String key) {
		return null;
	}

	/**
     * Returns the step duration.
     * 
     * @return the step duration
     */
    @Override
	public Duration step() {
		return get(AtlasProperties::getStep, AtlasConfig.super::step);
	}

	/**
     * Returns a boolean value indicating whether the feature is enabled.
     *
     * @return {@code true} if the feature is enabled, {@code false} otherwise.
     */
    @Override
	public boolean enabled() {
		return get(AtlasProperties::isEnabled, AtlasConfig.super::enabled);
	}

	/**
     * Returns the connect timeout duration.
     *
     * @return the connect timeout duration
     */
    @Override
	public Duration connectTimeout() {
		return get(AtlasProperties::getConnectTimeout, AtlasConfig.super::connectTimeout);
	}

	/**
     * Returns the read timeout duration.
     *
     * @return the read timeout duration
     */
    @Override
	public Duration readTimeout() {
		return get(AtlasProperties::getReadTimeout, AtlasConfig.super::readTimeout);
	}

	/**
     * Returns the number of threads.
     * 
     * @return the number of threads
     */
    @Override
	public int numThreads() {
		return get(AtlasProperties::getNumThreads, AtlasConfig.super::numThreads);
	}

	/**
     * Returns the batch size for processing.
     * 
     * @return the batch size
     */
    @Override
	public int batchSize() {
		return get(AtlasProperties::getBatchSize, AtlasConfig.super::batchSize);
	}

	/**
     * Returns the URI value from the AtlasProperties configuration, or falls back to the default URI value from the AtlasConfig interface.
     * 
     * @return the URI value
     */
    @Override
	public String uri() {
		return get(AtlasProperties::getUri, AtlasConfig.super::uri);
	}

	/**
     * Returns the time to live (TTL) for meter data.
     * 
     * @return the time to live for meter data
     */
    @Override
	public Duration meterTTL() {
		return get(AtlasProperties::getMeterTimeToLive, AtlasConfig.super::meterTTL);
	}

	/**
     * Returns whether the LWC (Lightweight Configuration) is enabled.
     * 
     * @return true if LWC is enabled, false otherwise
     */
    @Override
	public boolean lwcEnabled() {
		return get(AtlasProperties::isLwcEnabled, AtlasConfig.super::lwcEnabled);
	}

	/**
     * Returns the duration of the LWC step.
     * 
     * @return the duration of the LWC step
     */
    @Override
	public Duration lwcStep() {
		return get(AtlasProperties::getLwcStep, AtlasConfig.super::lwcStep);
	}

	/**
     * Returns a boolean value indicating whether to ignore the publish step in LWC.
     * 
     * @return {@code true} if the publish step should be ignored, {@code false} otherwise.
     */
    @Override
	public boolean lwcIgnorePublishStep() {
		return get(AtlasProperties::isLwcIgnorePublishStep, AtlasConfig.super::lwcIgnorePublishStep);
	}

	/**
     * Returns the refresh frequency for the configuration.
     *
     * @return the refresh frequency for the configuration
     */
    @Override
	public Duration configRefreshFrequency() {
		return get(AtlasProperties::getConfigRefreshFrequency, AtlasConfig.super::configRefreshFrequency);
	}

	/**
     * Returns the time to live (TTL) for the configuration.
     * 
     * @return the time to live (TTL) for the configuration
     */
    @Override
	public Duration configTTL() {
		return get(AtlasProperties::getConfigTimeToLive, AtlasConfig.super::configTTL);
	}

	/**
     * Returns the configuration URI.
     *
     * @return the configuration URI
     */
    @Override
	public String configUri() {
		return get(AtlasProperties::getConfigUri, AtlasConfig.super::configUri);
	}

	/**
     * Returns the evaluated URI.
     * 
     * @return the evaluated URI
     */
    @Override
	public String evalUri() {
		return get(AtlasProperties::getEvalUri, AtlasConfig.super::evalUri);
	}

}
