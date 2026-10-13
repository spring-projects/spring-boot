/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.ganglia;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import info.ganglia.gmetric4j.gmetric.GMetric;
import io.micrometer.ganglia.GangliaConfig;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.micrometer.metrics.autoconfigure.export.properties.PropertiesConfigAdapter;

/**
 * Adapter to convert {@link GangliaProperties} to a {@link GangliaConfig}.
 *
 * @author Jon Schneider
 * @author Phillip Webb
 */
class GangliaPropertiesConfigAdapter extends PropertiesConfigAdapter<GangliaProperties> implements GangliaConfig {

	GangliaPropertiesConfigAdapter(GangliaProperties properties) {
		super(properties);
	}

	@Override
	public String prefix() {
		return "management.ganglia.metrics.export";
	}

	@Override
	public @Nullable String get(String k) {
		return null;
	}

	@Override
	public boolean enabled() {
		return obtain(GangliaProperties::isEnabled, GangliaConfig.super::enabled);
	}

	@Override
	public Duration step() {
		return obtain(GangliaProperties::getStep, GangliaConfig.super::step);
	}

	@Override
	public TimeUnit durationUnits() {
		return obtain(GangliaProperties::getDurationUnits, GangliaConfig.super::durationUnits);
	}

	@Override
	public GMetric.UDPAddressingMode addressingMode() {
		return obtain(GangliaProperties::getAddressingMode, GangliaConfig.super::addressingMode);
	}

	@Override
	public int ttl() {
		return obtain(GangliaProperties::getTimeToLive, GangliaConfig.super::ttl);
	}

	@Override
	public String host() {
		return obtain(GangliaProperties::getHost, GangliaConfig.super::host);
	}

	@Override
	public int port() {
		return obtain(GangliaProperties::getPort, GangliaConfig.super::port);
	}

}
