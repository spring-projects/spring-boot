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

package org.springframework.boot.health.contributor;

import java.util.Collections;
import java.util.Iterator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HealthContributorsAdapter}.
 *
 * @author Phillip Webb
 */
class HealthContributorsAdapterTests {

	@Test
	void iteratorWhenDelegateContainsHealthIndicatorAdaptsDelegate() {
		HealthIndicator indicator = () -> Health.up().withDetail("spring", "boot").build();
		CompositeHealthContributor delegate = CompositeHealthContributor
			.fromMap(Collections.singletonMap("test", indicator));
		HealthContributorsAdapter adapter = createAdapter(delegate);
		Iterator<ReactiveHealthContributors.Entry> iterator = adapter.iterator();
		assertThat(iterator.hasNext()).isTrue();
		ReactiveHealthContributors.Entry adapted = iterator.next();
		assertThat(adapted.name()).isEqualTo("test");
		assertThat(adapted.contributor()).isInstanceOf(ReactiveHealthIndicator.class);
		Health health = ((ReactiveHealthIndicator) adapted.contributor()).health(true).block();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsEntry("spring", "boot");
	}

	@Test
	void iteratorWhenDelegateContainsCompositeHealthContributorAdaptsDelegate() {
		HealthIndicator indicator = () -> Health.up().withDetail("spring", "boot").build();
		CompositeHealthContributor composite = CompositeHealthContributor
			.fromMap(Collections.singletonMap("test1", indicator));
		CompositeHealthContributor delegate = CompositeHealthContributor
			.fromMap(Collections.singletonMap("test2", composite));
		HealthContributorsAdapter adapter = createAdapter(delegate);
		Iterator<ReactiveHealthContributors.Entry> iterator = adapter.iterator();
		assertThat(iterator.hasNext()).isTrue();
		ReactiveHealthContributors.Entry adapted = iterator.next();
		assertThat(adapted.name()).isEqualTo("test2");
		assertThat(adapted.contributor()).isInstanceOf(CompositeReactiveHealthContributor.class);
		ReactiveHealthContributor nested = ((CompositeReactiveHealthContributor) adapted.contributor())
			.getContributor("test1");
		Health health = ((ReactiveHealthIndicator) nested).health(true).block();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsEntry("spring", "boot");
	}

	@Test
	void getContributorAdaptsDelegate() {
		HealthIndicator indicator = () -> Health.up().withDetail("spring", "boot").build();
		CompositeHealthContributor delegate = CompositeHealthContributor
			.fromMap(Collections.singletonMap("test", indicator));
		HealthContributorsAdapter adapter = createAdapter(delegate);
		ReactiveHealthContributor adapted = adapter.getContributor("test");
		Health health = ((ReactiveHealthIndicator) adapted).health(true).block();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsEntry("spring", "boot");
	}

	protected HealthContributorsAdapter createAdapter(CompositeHealthContributor delegate) {
		return new CompositeHealthContributorAdapter(delegate);
	}

}
