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

package org.springframework.boot.actuate.autoconfigure.metrics.export.simple;

import java.time.Duration;

import io.micrometer.core.instrument.simple.CountingMode;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.AbstractPropertiesConfigAdapterTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SimplePropertiesConfigAdapter}.
 *
 * @author Mirko Sobeck
 */
class SimplePropertiesConfigAdapterTests
		extends AbstractPropertiesConfigAdapterTests<SimpleProperties, SimplePropertiesConfigAdapter> {

	SimplePropertiesConfigAdapterTests() {
		super(SimplePropertiesConfigAdapter.class);
	}

	@Test
	void whenPropertiesStepIsSetAdapterStepReturnsIt() {
		SimpleProperties properties = new SimpleProperties();
		properties.setStep(Duration.ofSeconds(30));
		assertThat(new SimplePropertiesConfigAdapter(properties).step()).isEqualTo(Duration.ofSeconds(30));
	}

	@Test
	void whenPropertiesModeIsSetAdapterModeReturnsIt() {
		SimpleProperties properties = new SimpleProperties();
		properties.setMode(CountingMode.STEP);
		assertThat(new SimplePropertiesConfigAdapter(properties).mode()).isEqualTo(CountingMode.STEP);
	}

}
