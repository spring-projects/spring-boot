/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.export.statsd;

import java.time.Duration;

import io.micrometer.statsd.StatsdFlavor;
import io.micrometer.statsd.StatsdProtocol;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.AbstractPropertiesConfigAdapterTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StatsdPropertiesConfigAdapter}.
 *
 * @author Johnny Lim
 */
class StatsdPropertiesConfigAdapterTests
		extends AbstractPropertiesConfigAdapterTests<StatsdProperties, StatsdPropertiesConfigAdapter> {

	protected StatsdPropertiesConfigAdapterTests() {
		super(StatsdPropertiesConfigAdapter.class);
	}

	@Test
	void whenPropertiesEnabledIsSetAdapterEnabledReturnsIt() {
		StatsdProperties properties = new StatsdProperties();
		properties.setEnabled(false);
		assertThat(new StatsdPropertiesConfigAdapter(properties).enabled()).isEqualTo(properties.isEnabled());
	}

	@Test
	void whenPropertiesFlavorIsSetAdapterFlavorReturnsIt() {
		StatsdProperties properties = new StatsdProperties();
		properties.setFlavor(StatsdFlavor.ETSY);
		assertThat(new StatsdPropertiesConfigAdapter(properties).flavor()).isEqualTo(properties.getFlavor());
	}

	@Test
	void whenPropertiesHostIsSetAdapterHostReturnsIt() {
		StatsdProperties properties = new StatsdProperties();
		properties.setHost("my-host");
		assertThat(new StatsdPropertiesConfigAdapter(properties).host()).isEqualTo(properties.getHost());
	}

	@Test
	void whenPropertiesPortIsSetAdapterPortReturnsIt() {
		StatsdProperties properties = new StatsdProperties();
		properties.setPort(1234);
		assertThat(new StatsdPropertiesConfigAdapter(properties).port()).isEqualTo(properties.getPort());
	}

	@Test
	void whenPropertiesProtocolIsSetAdapterProtocolReturnsIt() {
		StatsdProperties properties = new StatsdProperties();
		properties.setProtocol(StatsdProtocol.TCP);
		assertThat(new StatsdPropertiesConfigAdapter(properties).protocol()).isEqualTo(properties.getProtocol());
	}

	@Test
	void whenPropertiesMaxPacketLengthIsSetAdapterMaxPacketLengthReturnsIt() {
		StatsdProperties properties = new StatsdProperties();
		properties.setMaxPacketLength(1234);
		assertThat(new StatsdPropertiesConfigAdapter(properties).maxPacketLength())
			.isEqualTo(properties.getMaxPacketLength());
	}

	@Test
	void whenPropertiesPollingFrequencyIsSetAdapterPollingFrequencyReturnsIt() {
		StatsdProperties properties = new StatsdProperties();
		properties.setPollingFrequency(Duration.ofSeconds(1));
		assertThat(new StatsdPropertiesConfigAdapter(properties).pollingFrequency())
			.isEqualTo(properties.getPollingFrequency());
	}

	@Test
	void whenPropertiesStepIsSetAdapterStepReturnsIt() {
		StatsdProperties properties = new StatsdProperties();
		properties.setStep(Duration.ofSeconds(1));
		assertThat(new StatsdPropertiesConfigAdapter(properties).step()).isEqualTo(properties.getStep());
	}

	@Test
	void whenPropertiesPublishUnchangedMetersIsSetAdapterPublishUnchangedMetersReturnsIt() {
		StatsdProperties properties = new StatsdProperties();
		properties.setPublishUnchangedMeters(false);
		assertThat(new StatsdPropertiesConfigAdapter(properties).publishUnchangedMeters())
			.isEqualTo(properties.isPublishUnchangedMeters());
	}

	@Test
	void whenPropertiesBufferedIsSetAdapterBufferedReturnsIt() {
		StatsdProperties properties = new StatsdProperties();
		properties.setBuffered(false);
		assertThat(new StatsdPropertiesConfigAdapter(properties).buffered()).isEqualTo(properties.isBuffered());
	}

}
