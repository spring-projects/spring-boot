package org.springframework.boot.actuate.autoconfigure.metrics.export.statsd;

import java.time.Duration;

import org.springframework.boot.actuate.autoconfigure.metrics.export.PropertiesConfigAdapter;

import io.micrometer.statsd.StatsdConfig;
import io.micrometer.statsd.StatsdFlavor;

/**
 * Adapter to convert {@link StatsdProperties} to a {@link StatsdConfig}.
 *
 * @author Jon Schneider
 * @since 2.0.0
 */
public class StatsdPropertiesConfigAdapter extends
		PropertiesConfigAdapter<StatsdProperties, StatsdConfig> implements StatsdConfig {

	private static final StatsdConfig DEFAULTS = (key) -> null;

	public StatsdPropertiesConfigAdapter(StatsdProperties properties) {
		super(properties, DEFAULTS);
	}

	@Override
	public String get(String s) {
		return null;
	}

	@Override
	public StatsdFlavor flavor() {
		return get(StatsdProperties::getFlavor, StatsdConfig::flavor);
	}

	@Override
	public boolean enabled() {
		return get(StatsdProperties::getEnabled, StatsdConfig::enabled);
	}

	@Override
	public String host() {
		return get(StatsdProperties::getHost, StatsdConfig::host);
	}

	@Override
	public int port() {
		return get(StatsdProperties::getPort, StatsdConfig::port);
	}

	@Override
	public int maxPacketLength() {
		return get(StatsdProperties::getMaxPacketLength, StatsdConfig::maxPacketLength);
	}

	@Override
	public Duration pollingFrequency() {
		return get(StatsdProperties::getPollingFrequency, StatsdConfig::pollingFrequency);
	}

	@Override
	public int queueSize() {
		return get(StatsdProperties::getQueueSize, StatsdConfig::queueSize);
	}
}
