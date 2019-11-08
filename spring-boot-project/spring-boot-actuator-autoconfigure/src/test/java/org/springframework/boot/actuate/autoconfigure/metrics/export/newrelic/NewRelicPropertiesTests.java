/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.export.newrelic;

import io.micrometer.newrelic.NewRelicConfig;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryPropertiesTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NewRelicProperties}.
 *
 * @author Stephane Nicoll
 */
class NewRelicPropertiesTests extends StepRegistryPropertiesTests {

	@Test
	void defaultValuesAreConsistent() {
		NewRelicProperties properties = new NewRelicProperties();
		NewRelicConfig config = (key) -> null;
		assertStepRegistryDefaultValues(properties, config);
		// apiKey and account are mandatory
		assertThat(properties.getUri()).isEqualTo(config.uri());
		assertThat(properties.isMeterNameEventTypeEnabled()).isEqualTo(config.meterNameEventTypeEnabled());
	}

	@Test
	void eventTypeDefaultValueIsOverriden() {
		NewRelicProperties properties = new NewRelicProperties();
		NewRelicConfig config = (key) -> null;
		assertThat(properties.getEventType()).isNotEqualTo(config.eventType());
		assertThat(properties.getEventType()).isEqualTo("SpringBootSample");
		assertThat(config.eventType()).isEqualTo("MicrometerSample");
	}

}
