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

package org.springframework.boot.actuate.autoconfigure.metrics.export.properties;

import java.time.Duration;

import io.micrometer.core.instrument.push.PushRegistryConfig;

/**
 * Base class for {@link PushRegistryProperties} to {@link PushRegistryConfig} adapters.
 *
 * @param <T> the properties type
 * @author Jon Schneider
 * @author Phillip Webb
 * @author Artsiom Yudovin
 * @since 2.2.0
 */
public abstract class PushRegistryPropertiesConfigAdapter<T extends PushRegistryProperties>
		extends PropertiesConfigAdapter<T> implements PushRegistryConfig {

	/**
     * Constructs a new PushRegistryPropertiesConfigAdapter with the specified properties.
     * 
     * @param properties the properties to be used for configuring the adapter
     */
    public PushRegistryPropertiesConfigAdapter(T properties) {
		super(properties);
	}

	/**
     * Retrieves the value associated with the specified key from the PushRegistryPropertiesConfigAdapter.
     * 
     * @param k the key whose associated value is to be retrieved
     * @return the value to which the specified key is mapped, or null if the key is not found
     */
    @Override
	public String get(String k) {
		return null;
	}

	/**
     * Returns the step duration for the PushRegistryPropertiesConfigAdapter.
     * This method overrides the step() method in the PushRegistryConfig interface.
     * 
     * @return the step duration
     */
    @Override
	public Duration step() {
		return get(T::getStep, PushRegistryConfig.super::step);
	}

	/**
     * Returns the enabled status of the PushRegistryConfig.
     * 
     * @return true if the PushRegistryConfig is enabled, false otherwise.
     */
    @Override
	public boolean enabled() {
		return get(T::isEnabled, PushRegistryConfig.super::enabled);
	}

	/**
     * Returns the batch size for the push registry.
     * 
     * @return the batch size
     */
    @Override
	public int batchSize() {
		return get(T::getBatchSize, PushRegistryConfig.super::batchSize);
	}

}
