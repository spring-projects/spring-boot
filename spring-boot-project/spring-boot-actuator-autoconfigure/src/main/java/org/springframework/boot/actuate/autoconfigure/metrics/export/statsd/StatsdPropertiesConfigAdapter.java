/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.metrics.export.statsd;

import java.time.Duration;

import io.micrometer.statsd.StatsdConfig;
import io.micrometer.statsd.StatsdFlavor;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.PropertiesConfigAdapter;

/**
 * Adapter to convert {@link StatsdProperties} to a {@link StatsdConfig}.
 *
 * @author Jon Schneider
 * @since 2.0.0
 */
public class StatsdPropertiesConfigAdapter
		extends PropertiesConfigAdapter<StatsdProperties> implements StatsdConfig {

	public StatsdPropertiesConfigAdapter(StatsdProperties properties) {
		super(properties);
	}

	@Override
	public String get(String s) {
		return null;
	}

	@Override
	public StatsdFlavor flavor() {
		return get(StatsdProperties::getFlavor, StatsdConfig.super::flavor);
	}

	@Override
	public boolean enabled() {
		return get(StatsdProperties::isEnabled, StatsdConfig.super::enabled);
	}

	@Override
	public String host() {
		return get(StatsdProperties::getHost, StatsdConfig.super::host);
	}

	@Override
	public int port() {
		return get(StatsdProperties::getPort, StatsdConfig.super::port);
	}

	@Override
	public int maxPacketLength() {
		return get(StatsdProperties::getMaxPacketLength,
				StatsdConfig.super::maxPacketLength);
	}

	@Override
	public Duration pollingFrequency() {
		return get(StatsdProperties::getPollingFrequency,
				StatsdConfig.super::pollingFrequency);
	}

	@Override
	public boolean publishUnchangedMeters() {
		return get(StatsdProperties::isPublishUnchangedMeters,
				StatsdConfig.super::publishUnchangedMeters);
	}

}
