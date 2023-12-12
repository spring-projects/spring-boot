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

package org.springframework.boot.test.autoconfigure.actuate.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AutoConfigureObservability} when used on a sliced test.
 *
 * @author Moritz Halbritter
 */
@WebMvcTest
@AutoConfigureObservability
class AutoConfigureObservabilitySlicedIntegrationTests {

	@Autowired
	private ApplicationContext context;

	@Test
	void shouldHaveTracer() {
		assertThat(this.context.getBean(Tracer.class)).isEqualTo(Tracer.NOOP);
	}

	@Test
	void shouldHaveMeterRegistry() {
		assertThat(this.context.getBean(MeterRegistry.class)).isInstanceOf(SimpleMeterRegistry.class);
	}

	@Test
	void shouldHaveObservationRegistry() {
		assertThat(this.context.getBean(ObservationRegistry.class)).isNotNull();
	}

}
