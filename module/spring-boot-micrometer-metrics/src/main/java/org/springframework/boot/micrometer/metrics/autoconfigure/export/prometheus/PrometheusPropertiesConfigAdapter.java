/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.prometheus;

import java.time.Duration;
import java.util.Map;
import java.util.Properties;

import io.micrometer.prometheusmetrics.PrometheusConfig;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.micrometer.metrics.autoconfigure.export.properties.PropertiesConfigAdapter;

/**
 * Adapter to convert {@link PrometheusProperties} to a {@link PrometheusConfig}.
 *
 * @author Jon Schneider
 * @author Phillip Webb
 */
class PrometheusPropertiesConfigAdapter extends PropertiesConfigAdapter<PrometheusProperties>
		implements PrometheusConfig {

	PrometheusPropertiesConfigAdapter(PrometheusProperties properties) {
		super(properties);
	}

	@Override
	public String prefix() {
		return "management.prometheus.metrics.export";
	}

	@Override
	public @Nullable String get(String key) {
		return null;
	}

	@Override
	public boolean descriptions() {
		return obtain(PrometheusProperties::isDescriptions, PrometheusConfig.super::descriptions);
	}

	@Override
	public Duration step() {
		return obtain(PrometheusProperties::getStep, PrometheusConfig.super::step);
	}

	@Override
	public @Nullable Properties prometheusProperties() {
		return get(this::fromPropertiesMap, PrometheusConfig.super::prometheusProperties);
	}

	private @Nullable Properties fromPropertiesMap(PrometheusProperties prometheusProperties) {
		Map<String, String> additionalProperties = prometheusProperties.getProperties();
		if (additionalProperties.isEmpty()) {
			return null;
		}
		Properties properties = PrometheusConfig.super.prometheusProperties();
		if (properties == null) {
			properties = new Properties();
		}
		properties.putAll(additionalProperties);
		return properties;
	}

}
