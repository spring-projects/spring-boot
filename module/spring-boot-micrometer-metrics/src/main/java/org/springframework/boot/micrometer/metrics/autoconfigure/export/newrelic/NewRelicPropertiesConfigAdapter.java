/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.newrelic;

import io.micrometer.newrelic.ClientProviderType;
import io.micrometer.newrelic.NewRelicConfig;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.micrometer.metrics.autoconfigure.export.properties.StepRegistryPropertiesConfigAdapter;

/**
 * Adapter to convert {@link NewRelicProperties} to a {@link NewRelicConfig}.
 *
 * @author Jon Schneider
 * @author Neil Powell
 * @since 4.0.0
 */
public class NewRelicPropertiesConfigAdapter extends StepRegistryPropertiesConfigAdapter<NewRelicProperties>
		implements NewRelicConfig {

	public NewRelicPropertiesConfigAdapter(NewRelicProperties properties) {
		super(properties);
	}

	@Override
	public String prefix() {
		return "management.newrelic.metrics.export";
	}

	@Override
	public boolean meterNameEventTypeEnabled() {
		return obtain(NewRelicProperties::isMeterNameEventTypeEnabled, NewRelicConfig.super::meterNameEventTypeEnabled);
	}

	@Override
	public String eventType() {
		return obtain(NewRelicProperties::getEventType, NewRelicConfig.super::eventType);
	}

	@Override
	public ClientProviderType clientProviderType() {
		return obtain(NewRelicProperties::getClientProviderType, NewRelicConfig.super::clientProviderType);
	}

	@Override
	public @Nullable String apiKey() {
		return get(NewRelicProperties::getApiKey, NewRelicConfig.super::apiKey);
	}

	@Override
	public @Nullable String accountId() {
		return get(NewRelicProperties::getAccountId, NewRelicConfig.super::accountId);
	}

	@Override
	public String uri() {
		return obtain(NewRelicProperties::getUri, NewRelicConfig.super::uri);
	}

}
