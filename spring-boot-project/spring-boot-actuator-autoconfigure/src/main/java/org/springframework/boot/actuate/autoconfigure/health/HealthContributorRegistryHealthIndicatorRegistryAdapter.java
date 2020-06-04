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

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.NamedContributor;
import org.springframework.util.Assert;

/**
 * Adapter class to convert a {@link HealthContributorRegistry} to a legacy
 * {@link org.springframework.boot.actuate.health.HealthIndicatorRegistry}.
 *
 * @author Phillip Webb
 */
@SuppressWarnings("deprecation")
class HealthContributorRegistryHealthIndicatorRegistryAdapter
		implements org.springframework.boot.actuate.health.HealthIndicatorRegistry {

	private final HealthContributorRegistry contributorRegistry;

	HealthContributorRegistryHealthIndicatorRegistryAdapter(HealthContributorRegistry contributorRegistry) {
		Assert.notNull(contributorRegistry, "ContributorRegistry must not be null");
		this.contributorRegistry = contributorRegistry;
	}

	@Override
	public void register(String name, HealthIndicator healthIndicator) {
		this.contributorRegistry.registerContributor(name, healthIndicator);
	}

	@Override
	public HealthIndicator unregister(String name) {
		HealthContributor contributor = this.contributorRegistry.unregisterContributor(name);
		if (contributor instanceof HealthIndicator) {
			return (HealthIndicator) contributor;
		}
		return null;
	}

	@Override
	public HealthIndicator get(String name) {
		HealthContributor contributor = this.contributorRegistry.getContributor(name);
		if (contributor instanceof HealthIndicator) {
			return (HealthIndicator) contributor;
		}
		return null;
	}

	@Override
	public Map<String, HealthIndicator> getAll() {
		Map<String, HealthIndicator> all = new LinkedHashMap<>();
		for (NamedContributor<?> namedContributor : this.contributorRegistry) {
			if (namedContributor.getContributor() instanceof HealthIndicator) {
				all.put(namedContributor.getName(), (HealthIndicator) namedContributor.getContributor());
			}
		}
		return all;
	}

}
