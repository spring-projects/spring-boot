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

package org.springframework.boot.actuate.health;

import java.util.Collections;
import java.util.Iterator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link CompositeHealthContributorReactiveAdapter}.
 *
 * @author Phillip Webb
 */
class CompositeHealthContributorReactiveAdapterTests {

	@Test
	void createWhenDelegateIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new CompositeHealthContributorReactiveAdapter(null))
			.withMessage("Delegate must not be null");
	}

	@Test
	void iteratorWhenDelegateContainsHealthIndicatorAdaptsDelegate() {
		HealthIndicator indicator = () -> Health.up().withDetail("spring", "boot").build();
		CompositeHealthContributor delegate = CompositeHealthContributor
			.fromMap(Collections.singletonMap("test", indicator));
		CompositeHealthContributorReactiveAdapter adapter = new CompositeHealthContributorReactiveAdapter(delegate);
		Iterator<NamedContributor<ReactiveHealthContributor>> iterator = adapter.iterator();
		assertThat(iterator.hasNext()).isTrue();
		NamedContributor<ReactiveHealthContributor> adapted = iterator.next();
		assertThat(adapted.getName()).isEqualTo("test");
		assertThat(adapted.getContributor()).isInstanceOf(ReactiveHealthIndicator.class);
		Health health = ((ReactiveHealthIndicator) adapted.getContributor()).getHealth(true).block();
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
		CompositeHealthContributorReactiveAdapter adapter = new CompositeHealthContributorReactiveAdapter(delegate);
		Iterator<NamedContributor<ReactiveHealthContributor>> iterator = adapter.iterator();
		assertThat(iterator.hasNext()).isTrue();
		NamedContributor<ReactiveHealthContributor> adapted = iterator.next();
		assertThat(adapted.getName()).isEqualTo("test2");
		assertThat(adapted.getContributor()).isInstanceOf(CompositeReactiveHealthContributor.class);
		ReactiveHealthContributor nested = ((CompositeReactiveHealthContributor) adapted.getContributor())
			.getContributor("test1");
		Health health = ((ReactiveHealthIndicator) nested).getHealth(true).block();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsEntry("spring", "boot");
	}

	@Test
	void getContributorAdaptsDelegate() {
		HealthIndicator indicator = () -> Health.up().withDetail("spring", "boot").build();
		CompositeHealthContributor delegate = CompositeHealthContributor
			.fromMap(Collections.singletonMap("test", indicator));
		CompositeHealthContributorReactiveAdapter adapter = new CompositeHealthContributorReactiveAdapter(delegate);
		ReactiveHealthContributor adapted = adapter.getContributor("test");
		Health health = ((ReactiveHealthIndicator) adapted).getHealth(true).block();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsEntry("spring", "boot");
	}

}
