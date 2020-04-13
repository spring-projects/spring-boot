/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.availability;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.availability.AvailabilityProbesHealthEndpointGroupsRegistrar;
import org.springframework.boot.actuate.health.HealthEndpointGroup;
import org.springframework.boot.actuate.health.HealthEndpointGroupsRegistry;
import org.springframework.boot.actuate.health.TestHealthEndpointGroupsRegistry;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@code ProbesHealthEndpointGroupsRegistrar}
 *
 * @author Brian Clozel
 */
class AvailabilityProbesHealthEndpointGroupsRegistrarTests {

	private AvailabilityProbesHealthEndpointGroupsRegistrar registrar = new AvailabilityProbesHealthEndpointGroupsRegistrar();

	@Test
	void shouldAddKubernetesProbes() {
		HealthEndpointGroupsRegistry registry = new TestHealthEndpointGroupsRegistry();
		this.registrar.customize(registry);
		HealthEndpointGroup liveness = registry.get("liveness");
		assertThat(liveness).isNotNull();
		assertThat(liveness.isMember("livenessProbe")).isTrue();
		HealthEndpointGroup readiness = registry.get("readiness");
		assertThat(readiness).isNotNull();
		assertThat(readiness.isMember("readinessProbe")).isTrue();
	}

	@Test
	void shouldNotAddProbeIfGroupExists() {
		HealthEndpointGroupsRegistry registry = new TestHealthEndpointGroupsRegistry();
		registry.add("readiness", (configurer) -> configurer.include("test"));
		this.registrar.customize(registry);

		HealthEndpointGroup liveness = registry.get("liveness");
		assertThat(liveness).isNotNull();
		assertThat(liveness.isMember("livenessProbe")).isTrue();

		HealthEndpointGroup readiness = registry.get("readiness");
		assertThat(readiness).isNotNull();
		assertThat(readiness.isMember("readinessProbe")).isFalse();
		assertThat(readiness.isMember("test")).isTrue();
	}

}
