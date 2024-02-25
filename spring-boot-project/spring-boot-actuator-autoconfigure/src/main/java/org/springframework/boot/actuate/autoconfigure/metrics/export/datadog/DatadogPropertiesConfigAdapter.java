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

package org.springframework.boot.actuate.autoconfigure.metrics.export.datadog;

import io.micrometer.datadog.DatadogConfig;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryPropertiesConfigAdapter;

/**
 * Adapter to convert {@link DatadogProperties} to a {@link DatadogConfig}.
 *
 * @author Jon Schneider
 * @author Phillip Webb
 */
class DatadogPropertiesConfigAdapter extends StepRegistryPropertiesConfigAdapter<DatadogProperties>
		implements DatadogConfig {

	/**
	 * Constructs a new DatadogPropertiesConfigAdapter with the specified
	 * DatadogProperties.
	 * @param properties the DatadogProperties to be used for configuring the adapter
	 */
	DatadogPropertiesConfigAdapter(DatadogProperties properties) {
		super(properties);
	}

	/**
	 * Returns the prefix for exporting metrics to Datadog. The prefix is used to
	 * configure the property key for exporting metrics.
	 * @return the prefix for exporting metrics to Datadog
	 */
	@Override
	public String prefix() {
		return "management.datadog.metrics.export";
	}

	/**
	 * Returns the API key for the Datadog configuration.
	 * @return the API key
	 */
	@Override
	public String apiKey() {
		return get(DatadogProperties::getApiKey, DatadogConfig.super::apiKey);
	}

	/**
	 * Returns the application key for the Datadog configuration.
	 * @return the application key
	 */
	@Override
	public String applicationKey() {
		return get(DatadogProperties::getApplicationKey, DatadogConfig.super::applicationKey);
	}

	/**
	 * Returns the host tag for the Datadog configuration.
	 * @return the host tag
	 */
	@Override
	public String hostTag() {
		return get(DatadogProperties::getHostTag, DatadogConfig.super::hostTag);
	}

	/**
	 * Returns the URI for the Datadog properties.
	 * @return the URI for the Datadog properties
	 */
	@Override
	public String uri() {
		return get(DatadogProperties::getUri, DatadogConfig.super::uri);
	}

	/**
	 * Returns the value of the descriptions property.
	 * @return true if descriptions are enabled, false otherwise
	 */
	@Override
	public boolean descriptions() {
		return get(DatadogProperties::isDescriptions, DatadogConfig.super::descriptions);
	}

}
