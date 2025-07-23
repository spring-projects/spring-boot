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

package org.springframework.boot.health.registry;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.boot.health.contributor.HealthContributors;
import org.springframework.boot.health.contributor.HealthContributors.Entry;
import org.springframework.boot.health.contributor.HealthIndicator;

import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DefaultHealthContributorRegistry}.
 *
 * @author Phillip Webb
 */
class DefaultHealthContributorRegistryTests
		extends AbstractHealthContributorRegistryTests<HealthContributor, HealthContributors.Entry> {

	@Override
	protected AbstractRegistry<HealthContributor, Entry> createRegistry(
			Collection<? extends HealthContributorNameValidator> nameValidators,
			Consumer<BiConsumer<String, HealthContributor>> initialRegistrations) {
		return new DefaultHealthContributorRegistry(nameValidators, initialRegistrations);
	}

	@Override
	protected HealthContributor mockHealthIndicator() {
		return mock(HealthIndicator.class);
	}

	@Override
	protected String name(Entry entry) {
		return entry.name();
	}

	@Override
	protected HealthContributor contributor(Entry entry) {
		return entry.contributor();
	}

}
