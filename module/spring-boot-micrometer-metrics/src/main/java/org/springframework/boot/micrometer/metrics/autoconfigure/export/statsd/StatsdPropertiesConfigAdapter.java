/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.statsd;

import java.time.Duration;

import io.micrometer.statsd.StatsdConfig;
import io.micrometer.statsd.StatsdFlavor;
import io.micrometer.statsd.StatsdProtocol;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.micrometer.metrics.autoconfigure.export.properties.PropertiesConfigAdapter;

/**
 * Adapter to convert {@link StatsdProperties} to a {@link StatsdConfig}.
 *
 * @author Jon Schneider
 * @since 4.0.0
 */
public class StatsdPropertiesConfigAdapter extends PropertiesConfigAdapter<StatsdProperties> implements StatsdConfig {

	public StatsdPropertiesConfigAdapter(StatsdProperties properties) {
		super(properties);
	}

	@Override
	public @Nullable String get(String s) {
		return null;
	}

	@Override
	public String prefix() {
		return "management.statsd.metrics.export";
	}

	@Override
	public StatsdFlavor flavor() {
		return obtain(StatsdProperties::getFlavor, StatsdConfig.super::flavor);
	}

	@Override
	public boolean enabled() {
		return obtain(StatsdProperties::isEnabled, StatsdConfig.super::enabled);
	}

	@Override
	public String host() {
		return obtain(StatsdProperties::getHost, StatsdConfig.super::host);
	}

	@Override
	public int port() {
		return obtain(StatsdProperties::getPort, StatsdConfig.super::port);
	}

	@Override
	public StatsdProtocol protocol() {
		return obtain(StatsdProperties::getProtocol, StatsdConfig.super::protocol);
	}

	@Override
	public int maxPacketLength() {
		return obtain(StatsdProperties::getMaxPacketLength, StatsdConfig.super::maxPacketLength);
	}

	@Override
	public Duration pollingFrequency() {
		return obtain(StatsdProperties::getPollingFrequency, StatsdConfig.super::pollingFrequency);
	}

	@Override
	public Duration step() {
		return obtain(StatsdProperties::getStep, StatsdConfig.super::step);
	}

	@Override
	public boolean publishUnchangedMeters() {
		return obtain(StatsdProperties::isPublishUnchangedMeters, StatsdConfig.super::publishUnchangedMeters);
	}

	@Override
	public boolean buffered() {
		return obtain(StatsdProperties::isBuffered, StatsdConfig.super::buffered);
	}

}
