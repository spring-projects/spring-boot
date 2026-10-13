/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.humio;

import java.util.Map;

import io.micrometer.humio.HumioConfig;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.micrometer.metrics.autoconfigure.export.properties.StepRegistryPropertiesConfigAdapter;

/**
 * Adapter to convert {@link HumioProperties} to a {@link HumioConfig}.
 *
 * @author Andy Wilkinson
 */
class HumioPropertiesConfigAdapter extends StepRegistryPropertiesConfigAdapter<HumioProperties> implements HumioConfig {

	HumioPropertiesConfigAdapter(HumioProperties properties) {
		super(properties);
	}

	@Override
	public String prefix() {
		return "management.humio.metrics.export";
	}

	@Override
	public @Nullable String get(String k) {
		return null;
	}

	@Override
	public String uri() {
		return obtain(HumioProperties::getUri, HumioConfig.super::uri);
	}

	@Override
	public @Nullable Map<String, String> tags() {
		return get(HumioProperties::getTags, HumioConfig.super::tags);
	}

	@Override
	public @Nullable String apiToken() {
		return get(HumioProperties::getApiToken, HumioConfig.super::apiToken);
	}

}
