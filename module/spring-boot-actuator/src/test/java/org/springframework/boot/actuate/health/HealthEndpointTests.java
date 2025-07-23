/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.actuate.health;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.actuate.health.HealthEndpointSupport.Result;
import org.springframework.boot.health.contributor.CompositeHealthContributor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.health.registry.DefaultHealthContributorRegistry;
import org.springframework.boot.health.registry.HealthContributorRegistry;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HealthEndpoint}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
@ExtendWith(OutputCaptureExtension.class)
class HealthEndpointTests extends
		HealthEndpointSupportTests<HealthEndpoint, Health, HealthDescriptor, HealthContributorRegistry, HealthContributor> {

	@Test
	void healthReturnsSystemHealth() {
		HealthContributorRegistry registry = createRegistry("test", createContributor(this.up));
		HealthEndpoint endpoint = create(registry, this.groups);
		HealthDescriptor descriptor = endpoint.health();
		assertThat(descriptor.getStatus()).isEqualTo(Status.UP);
		assertThat(descriptor).isInstanceOf(SystemHealthDescriptor.class);
	}

	@Test
	void healthWithNoContributorReturnsUp() {
		HealthContributorRegistry registry = createRegistry(null);
		HealthEndpointGroups groups = HealthEndpointGroups.of(mock(HealthEndpointGroup.class), Collections.emptyMap());
		HealthEndpoint endpoint = create(registry, groups);
		HealthDescriptor descriptor = endpoint.health();
		assertThat(descriptor.getStatus()).isEqualTo(Status.UP);
		assertThat(descriptor).isInstanceOf(IndicatedHealthDescriptor.class);
	}

	@Test
	void healthWhenPathDoesNotExistReturnsNull() {
		HealthContributorRegistry registry = createRegistry("test", createContributor(this.up));
		HealthEndpoint endpoint = create(registry, this.groups);
		HealthDescriptor descriptor = endpoint.healthForPath("missing");
		assertThat(descriptor).isNull();
	}

	@Test
	void healthWhenPathExistsReturnsHealth() {
		HealthContributorRegistry registry = createRegistry("test", createContributor(this.up));
		HealthEndpoint endpoint = create(registry, this.groups);
		IndicatedHealthDescriptor descriptor = (IndicatedHealthDescriptor) endpoint.healthForPath("test");
		assertThat(descriptor.getStatus()).isEqualTo(Status.UP);
		assertThat(descriptor.getDetails()).containsEntry("spring", "boot");
	}

	@Test
	void healthWhenIndicatorIsSlow(CapturedOutput output) {
		HealthIndicator indicator = () -> {
			try {
				Thread.sleep(100);
			}
			catch (InterruptedException ex) {
				// Ignore
			}
			return this.up;
		};
		HealthContributorRegistry registry = createRegistry("test", indicator);
		HealthEndpoint endpoint = create(registry, this.groups, Duration.ofMillis(10));
		endpoint.health();
		assertThat(output).contains("Health contributor");
		assertThat(output).contains("to respond");
	}

	@Override
	protected HealthEndpoint create(HealthContributorRegistry registry, HealthEndpointGroups groups,
			Duration slowContributorLoggingThreshold) {
		return new HealthEndpoint(registry, null, groups, slowContributorLoggingThreshold);
	}

	@Override
	protected HealthContributorRegistry createRegistry(
			Consumer<BiConsumer<String, HealthContributor>> initialRegistrations) {
		return new DefaultHealthContributorRegistry(Collections.emptyList(), initialRegistrations);
	}

	@Override
	protected HealthContributor createContributor(Health health) {
		return (HealthIndicator) () -> health;
	}

	@Override
	protected HealthContributor createCompositeContributor(Map<String, HealthContributor> contributors) {
		return CompositeHealthContributor.fromMap(contributors);
	}

	@Override
	protected HealthDescriptor getDescriptor(Result<HealthDescriptor> result) {
		return result.descriptor();
	}

}
