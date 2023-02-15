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

package org.springframework.boot.actuate.autoconfigure.metrics.export.graphite;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import io.micrometer.graphite.GraphiteProtocol;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.AbstractPropertiesConfigAdapterTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GraphitePropertiesConfigAdapter}.
 *
 * @author Mirko Sobeck
 */
class GraphitePropertiesConfigAdapterTests
		extends AbstractPropertiesConfigAdapterTests<GraphiteProperties, GraphitePropertiesConfigAdapter> {

	GraphitePropertiesConfigAdapterTests() {
		super(GraphitePropertiesConfigAdapter.class);
	}

	@Test
	void whenPropertiesEnabledIsSetAdapterEnabledReturnsIt() {
		GraphiteProperties properties = new GraphiteProperties();
		properties.setEnabled(false);
		assertThat(new GraphitePropertiesConfigAdapter(properties).enabled()).isFalse();
	}

	@Test
	void whenPropertiesStepIsSetAdapterStepReturnsIt() {
		GraphiteProperties properties = new GraphiteProperties();
		properties.setStep(Duration.ofMinutes(15));
		assertThat(new GraphitePropertiesConfigAdapter(properties).step()).isEqualTo(Duration.ofMinutes(15));
	}

	@Test
	void whenPropertiesRateUnitsIsSetAdapterRateUnitsReturnsIt() {
		GraphiteProperties properties = new GraphiteProperties();
		properties.setRateUnits(TimeUnit.MINUTES);
		assertThat(new GraphitePropertiesConfigAdapter(properties).rateUnits()).isEqualTo(TimeUnit.MINUTES);
	}

	@Test
	void whenPropertiesDurationUnitsIsSetAdapterDurationUnitsReturnsIt() {
		GraphiteProperties properties = new GraphiteProperties();
		properties.setDurationUnits(TimeUnit.MINUTES);
		assertThat(new GraphitePropertiesConfigAdapter(properties).durationUnits()).isEqualTo(TimeUnit.MINUTES);
	}

	@Test
	void whenPropertiesHostIsSetAdapterHostReturnsIt() {
		GraphiteProperties properties = new GraphiteProperties();
		properties.setHost("node");
		assertThat(new GraphitePropertiesConfigAdapter(properties).host()).isEqualTo("node");
	}

	@Test
	void whenPropertiesPortIsSetAdapterPortReturnsIt() {
		GraphiteProperties properties = new GraphiteProperties();
		properties.setPort(4242);
		assertThat(new GraphitePropertiesConfigAdapter(properties).port()).isEqualTo(4242);
	}

	@Test
	void whenPropertiesProtocolIsSetAdapterProtocolReturnsIt() {
		GraphiteProperties properties = new GraphiteProperties();
		properties.setProtocol(GraphiteProtocol.UDP);
		assertThat(new GraphitePropertiesConfigAdapter(properties).protocol()).isEqualTo(GraphiteProtocol.UDP);
	}

	@Test
	void whenPropertiesGraphiteTagsEnabledIsSetAdapterGraphiteTagsEnabledReturnsIt() {
		GraphiteProperties properties = new GraphiteProperties();
		properties.setGraphiteTagsEnabled(true);
		assertThat(new GraphitePropertiesConfigAdapter(properties).graphiteTagsEnabled()).isTrue();
	}

	@Test
	void whenPropertiesTagsAsPrefixIsSetAdapterTagsAsPrefixReturnsIt() {
		GraphiteProperties properties = new GraphiteProperties();
		properties.setTagsAsPrefix(new String[] { "worker" });
		assertThat(new GraphitePropertiesConfigAdapter(properties).tagsAsPrefix()).isEqualTo(new String[] { "worker" });
	}

}
