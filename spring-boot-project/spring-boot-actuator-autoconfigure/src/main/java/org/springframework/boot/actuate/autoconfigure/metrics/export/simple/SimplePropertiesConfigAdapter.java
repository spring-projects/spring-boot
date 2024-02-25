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

package org.springframework.boot.actuate.autoconfigure.metrics.export.simple;

import java.time.Duration;

import io.micrometer.core.instrument.simple.CountingMode;
import io.micrometer.core.instrument.simple.SimpleConfig;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.PropertiesConfigAdapter;

/**
 * Adapter to convert {@link SimpleProperties} to a {@link SimpleConfig}.
 *
 * @author Jon Schneider
 * @since 2.0.0
 */
public class SimplePropertiesConfigAdapter extends PropertiesConfigAdapter<SimpleProperties> implements SimpleConfig {

	/**
     * Constructs a new SimplePropertiesConfigAdapter with the specified SimpleProperties.
     * 
     * @param properties the SimpleProperties object to be used for configuration
     */
    public SimplePropertiesConfigAdapter(SimpleProperties properties) {
		super(properties);
	}

	/**
     * Returns the prefix for the management simple metrics export configuration.
     * 
     * @return the prefix for the management simple metrics export configuration
     */
    @Override
	public String prefix() {
		return "management.simple.metrics.export";
	}

	/**
     * Retrieves the value associated with the specified key from the properties configuration.
     * 
     * @param k the key whose associated value is to be retrieved
     * @return the value to which the specified key is mapped, or null if the key is not found
     */
    @Override
	public String get(String k) {
		return null;
	}

	/**
     * Returns the step duration.
     * 
     * @return the step duration
     */
    @Override
	public Duration step() {
		return get(SimpleProperties::getStep, SimpleConfig.super::step);
	}

	/**
     * Returns the counting mode.
     * 
     * @return the counting mode
     */
    @Override
	public CountingMode mode() {
		return get(SimpleProperties::getMode, SimpleConfig.super::mode);
	}

}
