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

package org.springframework.boot.actuate.autoconfigure.metrics.export.kairos;

import io.micrometer.kairos.KairosConfig;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryPropertiesConfigAdapter;

/**
 * Adapter to convert {@link KairosProperties} to a {@link KairosConfig}.
 *
 * @author Stephane Nicoll
 */
class KairosPropertiesConfigAdapter extends StepRegistryPropertiesConfigAdapter<KairosProperties>
		implements KairosConfig {

	/**
     * Constructs a new KairosPropertiesConfigAdapter with the specified KairosProperties.
     * 
     * @param properties the KairosProperties to be used for configuring the adapter
     */
    KairosPropertiesConfigAdapter(KairosProperties properties) {
		super(properties);
	}

	/**
     * Returns the prefix for Kairos metrics export configuration properties.
     *
     * @return the prefix for Kairos metrics export configuration properties
     */
    @Override
	public String prefix() {
		return "management.kairos.metrics.export";
	}

	/**
     * Returns the URI value from the KairosPropertiesConfigAdapter class.
     * If the URI value is not present in the KairosPropertiesConfigAdapter class,
     * it falls back to the default URI value provided by the KairosConfig interface.
     *
     * @return the URI value from the KairosPropertiesConfigAdapter class,
     *         or the default URI value from the KairosConfig interface if not present
     */
    @Override
	public String uri() {
		return get(KairosProperties::getUri, KairosConfig.super::uri);
	}

	/**
     * Returns the username for the Kairos configuration.
     * 
     * @return the username for the Kairos configuration
     */
    @Override
	public String userName() {
		return get(KairosProperties::getUserName, KairosConfig.super::userName);
	}

	/**
     * Returns the password value from the KairosPropertiesConfigAdapter class.
     * If the password value is not present in the KairosPropertiesConfigAdapter class,
     * it falls back to the password value from the KairosConfig interface.
     *
     * @return the password value
     */
    @Override
	public String password() {
		return get(KairosProperties::getPassword, KairosConfig.super::password);
	}

}
