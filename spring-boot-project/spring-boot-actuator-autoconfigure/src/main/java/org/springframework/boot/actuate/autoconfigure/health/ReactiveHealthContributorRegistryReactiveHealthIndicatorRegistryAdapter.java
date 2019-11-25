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

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.actuate.health.NamedContributor;
import org.springframework.boot.actuate.health.ReactiveHealthContributor;
import org.springframework.boot.actuate.health.ReactiveHealthContributorRegistry;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.boot.actuate.health.ReactiveHealthIndicatorRegistry;
import org.springframework.util.Assert;

/**
 * Adapter class to convert a {@link ReactiveHealthContributorRegistry} to a legacy
 * {@link ReactiveHealthIndicatorRegistry}.
 *
 * @author Phillip Webb
 */
@SuppressWarnings("deprecation")
class ReactiveHealthContributorRegistryReactiveHealthIndicatorRegistryAdapter
		implements ReactiveHealthIndicatorRegistry {

	private final ReactiveHealthContributorRegistry contributorRegistry;

	ReactiveHealthContributorRegistryReactiveHealthIndicatorRegistryAdapter(
			ReactiveHealthContributorRegistry contributorRegistry) {
		Assert.notNull(contributorRegistry, "ContributorRegistry must not be null");
		this.contributorRegistry = contributorRegistry;
	}

	@Override
	public void register(String name, ReactiveHealthIndicator healthIndicator) {
		this.contributorRegistry.registerContributor(name, healthIndicator);
	}

	@Override
	public ReactiveHealthIndicator unregister(String name) {
		ReactiveHealthContributor contributor = this.contributorRegistry.unregisterContributor(name);
		if (contributor instanceof ReactiveHealthIndicator) {
			return (ReactiveHealthIndicator) contributor;
		}
		return null;
	}

	@Override
	public ReactiveHealthIndicator get(String name) {
		ReactiveHealthContributor contributor = this.contributorRegistry.getContributor(name);
		if (contributor instanceof ReactiveHealthIndicator) {
			return (ReactiveHealthIndicator) contributor;
		}
		return null;
	}

	@Override
	public Map<String, ReactiveHealthIndicator> getAll() {
		Map<String, ReactiveHealthIndicator> all = new LinkedHashMap<>();
		for (NamedContributor<?> namedContributor : this.contributorRegistry) {
			if (namedContributor.getContributor() instanceof ReactiveHealthIndicator) {
				all.put(namedContributor.getName(), (ReactiveHealthIndicator) namedContributor.getContributor());
			}
		}
		return all;
	}

}
