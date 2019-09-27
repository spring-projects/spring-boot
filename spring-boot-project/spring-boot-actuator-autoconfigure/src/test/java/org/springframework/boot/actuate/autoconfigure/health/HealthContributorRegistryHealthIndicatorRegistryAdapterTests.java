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

package org.springframework.boot.actuate.autoconfigure.health;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.health.CompositeHealthContributor;
import org.springframework.boot.actuate.health.DefaultHealthContributorRegistry;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.boot.actuate.health.HealthIndicator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HealthContributorRegistryHealthIndicatorRegistryAdapter}.
 *
 * @author Phillip Webb
 */
class HealthContributorRegistryHealthIndicatorRegistryAdapterTests {

	private HealthContributorRegistry contributorRegistry = new DefaultHealthContributorRegistry();

	private HealthContributorRegistryHealthIndicatorRegistryAdapter adapter = new HealthContributorRegistryHealthIndicatorRegistryAdapter(
			this.contributorRegistry);

	@Test
	void createWhenContributorRegistryIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new HealthContributorRegistryHealthIndicatorRegistryAdapter(null))
				.withMessage("ContributorRegistry must not be null");
	}

	@Test
	void registerDelegatesToContributorRegistry() {
		HealthIndicator healthIndicator = mock(HealthIndicator.class);
		this.adapter.register("test", healthIndicator);
		assertThat(this.contributorRegistry.getContributor("test")).isSameAs(healthIndicator);
	}

	@Test
	void unregisterWhenDelegatesToContributorRegistry() {
		HealthIndicator healthIndicator = mock(HealthIndicator.class);
		this.contributorRegistry.registerContributor("test", healthIndicator);
		HealthIndicator unregistered = this.adapter.unregister("test");
		assertThat(unregistered).isSameAs(healthIndicator);
		assertThat(this.contributorRegistry.getContributor("test")).isNull();
	}

	@Test
	void unregisterWhenContributorRegistryResultIsNotHealthIndicatorReturnsNull() {
		CompositeHealthContributor healthContributor = mock(CompositeHealthContributor.class);
		this.contributorRegistry.registerContributor("test", healthContributor);
		HealthIndicator unregistered = this.adapter.unregister("test");
		assertThat(unregistered).isNull();
		assertThat(this.contributorRegistry.getContributor("test")).isNull();
	}

	@Test
	void getDelegatesToContributorRegistry() {
		HealthIndicator healthIndicator = mock(HealthIndicator.class);
		this.contributorRegistry.registerContributor("test", healthIndicator);
		assertThat(this.adapter.get("test")).isSameAs(healthIndicator);
	}

	@Test
	void getWhenContributorRegistryResultIsNotHealthIndicatorReturnsNull() {
		CompositeHealthContributor healthContributor = mock(CompositeHealthContributor.class);
		this.contributorRegistry.registerContributor("test", healthContributor);
		assertThat(this.adapter.get("test")).isNull();
	}

	@Test
	void getAllDelegatesContributorRegistry() {
		HealthIndicator healthIndicator = mock(HealthIndicator.class);
		this.contributorRegistry.registerContributor("test", healthIndicator);
		Map<String, HealthIndicator> all = this.adapter.getAll();
		assertThat(all).containsOnly(entry("test", healthIndicator));
	}

	@Test
	void getAllWhenContributorRegistryContainsNonHealthIndicatorInstancesReturnsFilteredMap() {
		CompositeHealthContributor healthContributor = mock(CompositeHealthContributor.class);
		this.contributorRegistry.registerContributor("test1", healthContributor);
		HealthIndicator healthIndicator = mock(HealthIndicator.class);
		this.contributorRegistry.registerContributor("test2", healthIndicator);
		Map<String, HealthIndicator> all = this.adapter.getAll();
		assertThat(all).containsOnly(entry("test2", healthIndicator));
	}

}
