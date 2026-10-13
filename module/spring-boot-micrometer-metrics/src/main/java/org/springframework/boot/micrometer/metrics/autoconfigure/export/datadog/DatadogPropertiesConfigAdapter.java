/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.datadog;

import io.micrometer.datadog.DatadogConfig;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.micrometer.metrics.autoconfigure.export.properties.StepRegistryPropertiesConfigAdapter;

/**
 * Adapter to convert {@link DatadogProperties} to a {@link DatadogConfig}.
 *
 * @author Jon Schneider
 * @author Phillip Webb
 */
class DatadogPropertiesConfigAdapter extends StepRegistryPropertiesConfigAdapter<DatadogProperties>
		implements DatadogConfig {

	DatadogPropertiesConfigAdapter(DatadogProperties properties) {
		super(properties);
	}

	@Override
	public String prefix() {
		return "management.datadog.metrics.export";
	}

	@Override
	public String apiKey() {
		return obtain(DatadogProperties::getApiKey, DatadogConfig.super::apiKey);
	}

	@Override
	public @Nullable String applicationKey() {
		return get(DatadogProperties::getApplicationKey, DatadogConfig.super::applicationKey);
	}

	@Override
	public boolean compress() {
		return obtain(DatadogProperties::isCompress, DatadogConfig.super::compress);
	}

	@Override
	public @Nullable String hostTag() {
		return get(DatadogProperties::getHostTag, DatadogConfig.super::hostTag);
	}

	@Override
	public String uri() {
		return obtain(DatadogProperties::getUri, DatadogConfig.super::uri);
	}

	@Override
	public boolean descriptions() {
		return obtain(DatadogProperties::isDescriptions, DatadogConfig.super::descriptions);
	}

}
