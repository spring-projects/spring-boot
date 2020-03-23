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

package org.springframework.boot.actuate.autoconfigure.health;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.health.CompositeReactiveHealthContributor;
import org.springframework.boot.actuate.health.DefaultReactiveHealthContributorRegistry;
import org.springframework.boot.actuate.health.ReactiveHealthContributorRegistry;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;

/**
 * Test for
 * {@link ReactiveHealthContributorRegistryReactiveHealthIndicatorRegistryAdapter}.
 *
 * @author Phillip Webb
 */
class ReactiveHealthContributorRegistryReactiveHealthIndicatorRegistryAdapterTests {

	private ReactiveHealthContributorRegistry contributorRegistry = new DefaultReactiveHealthContributorRegistry();

	private ReactiveHealthContributorRegistryReactiveHealthIndicatorRegistryAdapter adapter = new ReactiveHealthContributorRegistryReactiveHealthIndicatorRegistryAdapter(
			this.contributorRegistry);

	@Test
	void createWhenContributorRegistryIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ReactiveHealthContributorRegistryReactiveHealthIndicatorRegistryAdapter(null))
				.withMessage("ContributorRegistry must not be null");
	}

	@Test
	void registerDelegatesToContributorRegistry() {
		ReactiveHealthIndicator healthIndicator = mock(ReactiveHealthIndicator.class);
		this.adapter.register("test", healthIndicator);
		assertThat(this.contributorRegistry.getContributor("test")).isSameAs(healthIndicator);
	}

	@Test
	void unregisterDelegatesToContributorRegistry() {
		ReactiveHealthIndicator healthIndicator = mock(ReactiveHealthIndicator.class);
		this.contributorRegistry.registerContributor("test", healthIndicator);
		ReactiveHealthIndicator unregistered = this.adapter.unregister("test");
		assertThat(unregistered).isSameAs(healthIndicator);
		assertThat(this.contributorRegistry.getContributor("test")).isNull();
	}

	@Test
	void unregisterWhenContributorRegistryResultIsNotHealthIndicatorReturnsNull() {
		CompositeReactiveHealthContributor healthContributor = mock(CompositeReactiveHealthContributor.class);
		this.contributorRegistry.registerContributor("test", healthContributor);
		ReactiveHealthIndicator unregistered = this.adapter.unregister("test");
		assertThat(unregistered).isNull();
		assertThat(this.contributorRegistry.getContributor("test")).isNull();
	}

	@Test
	void getDelegatesToContributorRegistry() {
		ReactiveHealthIndicator healthIndicator = mock(ReactiveHealthIndicator.class);
		this.contributorRegistry.registerContributor("test", healthIndicator);
		assertThat(this.adapter.get("test")).isSameAs(healthIndicator);
	}

	@Test
	void getWhenContributorRegistryResultIsNotHealthIndicatorReturnsNull() {
		CompositeReactiveHealthContributor healthContributor = mock(CompositeReactiveHealthContributor.class);
		this.contributorRegistry.registerContributor("test", healthContributor);
		assertThat(this.adapter.get("test")).isNull();
	}

	@Test
	void getAllDelegatesToContributorRegistry() {
		ReactiveHealthIndicator healthIndicator = mock(ReactiveHealthIndicator.class);
		this.contributorRegistry.registerContributor("test", healthIndicator);
		Map<String, ReactiveHealthIndicator> all = this.adapter.getAll();
		assertThat(all).containsOnly(entry("test", healthIndicator));
	}

	@Test
	void getAllWhenContributorRegistryContainsNonHealthIndicatorInstancesReturnsFilteredMap() {
		CompositeReactiveHealthContributor healthContributor = mock(CompositeReactiveHealthContributor.class);
		this.contributorRegistry.registerContributor("test1", healthContributor);
		ReactiveHealthIndicator healthIndicator = mock(ReactiveHealthIndicator.class);
		this.contributorRegistry.registerContributor("test2", healthIndicator);
		Map<String, ReactiveHealthIndicator> all = this.adapter.getAll();
		assertThat(all).containsOnly(entry("test2", healthIndicator));
	}

}
