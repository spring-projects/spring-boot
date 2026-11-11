/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.jmx;

import java.time.Duration;

import io.micrometer.jmx.JmxConfig;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.micrometer.metrics.autoconfigure.export.properties.PropertiesConfigAdapter;

/**
 * Adapter to convert {@link JmxProperties} to a {@link JmxConfig}.
 *
 * @author Jon Schneider
 * @author Stephane Nicoll
 */
class JmxPropertiesConfigAdapter extends PropertiesConfigAdapter<JmxProperties> implements JmxConfig {

	JmxPropertiesConfigAdapter(JmxProperties properties) {
		super(properties);
	}

	@Override
	public String prefix() {
		return "management.jmx.metrics.export";
	}

	@Override
	public @Nullable String get(String key) {
		return null;
	}

	@Override
	public String domain() {
		return obtain(JmxProperties::getDomain, JmxConfig.super::domain);
	}

	@Override
	public Duration step() {
		return obtain(JmxProperties::getStep, JmxConfig.super::step);
	}

}
