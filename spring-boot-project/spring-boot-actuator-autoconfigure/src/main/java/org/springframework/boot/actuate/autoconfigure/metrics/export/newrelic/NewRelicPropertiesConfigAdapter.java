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

package org.springframework.boot.actuate.autoconfigure.metrics.export.newrelic;

import io.micrometer.newrelic.ClientProviderType;
import io.micrometer.newrelic.NewRelicConfig;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryPropertiesConfigAdapter;

/**
 * Adapter to convert {@link NewRelicProperties} to a {@link NewRelicConfig}.
 *
 * @author Jon Schneider
 * @author Neil Powell
 * @since 2.0.0
 */
public class NewRelicPropertiesConfigAdapter extends StepRegistryPropertiesConfigAdapter<NewRelicProperties>
		implements NewRelicConfig {

	/**
	 * Constructs a new NewRelicPropertiesConfigAdapter with the specified
	 * NewRelicProperties.
	 * @param properties the NewRelicProperties to be used for configuration
	 */
	public NewRelicPropertiesConfigAdapter(NewRelicProperties properties) {
		super(properties);
	}

	/**
	 * Returns the prefix for exporting New Relic metrics. The prefix is used to configure
	 * the property "management.newrelic.metrics.export".
	 * @return the prefix for exporting New Relic metrics
	 */
	@Override
	public String prefix() {
		return "management.newrelic.metrics.export";
	}

	/**
	 * Returns a boolean value indicating whether the meter name event type is enabled.
	 * @return {@code true} if the meter name event type is enabled, {@code false}
	 * otherwise
	 */
	@Override
	public boolean meterNameEventTypeEnabled() {
		return get(NewRelicProperties::isMeterNameEventTypeEnabled, NewRelicConfig.super::meterNameEventTypeEnabled);
	}

	/**
	 * Returns the event type for New Relic.
	 * @return the event type
	 */
	@Override
	public String eventType() {
		return get(NewRelicProperties::getEventType, NewRelicConfig.super::eventType);
	}

	/**
	 * Returns the client provider type for New Relic.
	 * @return the client provider type
	 */
	@Override
	public ClientProviderType clientProviderType() {
		return get(NewRelicProperties::getClientProviderType, NewRelicConfig.super::clientProviderType);
	}

	/**
	 * Returns the API key for New Relic.
	 * @return the API key for New Relic
	 */
	@Override
	public String apiKey() {
		return get(NewRelicProperties::getApiKey, NewRelicConfig.super::apiKey);
	}

	/**
	 * Returns the account ID for New Relic.
	 * @return the account ID
	 */
	@Override
	public String accountId() {
		return get(NewRelicProperties::getAccountId, NewRelicConfig.super::accountId);
	}

	/**
	 * Returns the URI value from the NewRelicPropertiesConfigAdapter class. If the URI
	 * value is not present in the NewRelicPropertiesConfigAdapter class, it falls back to
	 * the default URI value provided by the NewRelicConfig interface.
	 * @return the URI value from the NewRelicPropertiesConfigAdapter class, or the
	 * default URI value from the NewRelicConfig interface if not present
	 */
	@Override
	public String uri() {
		return get(NewRelicProperties::getUri, NewRelicConfig.super::uri);
	}

}
