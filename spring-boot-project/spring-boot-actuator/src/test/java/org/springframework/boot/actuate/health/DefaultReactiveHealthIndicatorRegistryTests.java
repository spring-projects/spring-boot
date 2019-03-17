/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.health;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DefaultReactiveHealthIndicatorRegistry}.
 *
 * @author Vedran Pavic
 * @author Stephane Nicoll
 */
public class DefaultReactiveHealthIndicatorRegistryTests {

	private ReactiveHealthIndicator one = mock(ReactiveHealthIndicator.class);

	private ReactiveHealthIndicator two = mock(ReactiveHealthIndicator.class);

	private DefaultReactiveHealthIndicatorRegistry registry;

	@Before
	public void setUp() {
		given(this.one.health()).willReturn(
				Mono.just(new Health.Builder().unknown().withDetail("1", "1").build()));
		given(this.two.health()).willReturn(
				Mono.just(new Health.Builder().unknown().withDetail("2", "2").build()));
		this.registry = new DefaultReactiveHealthIndicatorRegistry();
	}

	@Test
	public void register() {
		this.registry.register("one", this.one);
		this.registry.register("two", this.two);
		assertThat(this.registry.getAll()).hasSize(2);
		assertThat(this.registry.get("one")).isSameAs(this.one);
		assertThat(this.registry.get("two")).isSameAs(this.two);
	}

	@Test
	public void registerAlreadyUsedName() {
		this.registry.register("one", this.one);
		assertThatIllegalStateException()
				.isThrownBy(() -> this.registry.register("one", this.two))
				.withMessageContaining(
						"HealthIndicator with name 'one' already registered");
	}

	@Test
	public void unregister() {
		this.registry.register("one", this.one);
		this.registry.register("two", this.two);
		assertThat(this.registry.getAll()).hasSize(2);
		ReactiveHealthIndicator two = this.registry.unregister("two");
		assertThat(two).isSameAs(this.two);
		assertThat(this.registry.getAll()).hasSize(1);
	}

	@Test
	public void unregisterUnknown() {
		this.registry.register("one", this.one);
		assertThat(this.registry.getAll()).hasSize(1);
		ReactiveHealthIndicator two = this.registry.unregister("two");
		assertThat(two).isNull();
		assertThat(this.registry.getAll()).hasSize(1);
	}

	@Test
	public void getAllIsASnapshot() {
		this.registry.register("one", this.one);
		Map<String, ReactiveHealthIndicator> snapshot = this.registry.getAll();
		assertThat(snapshot).containsOnlyKeys("one");
		this.registry.register("two", this.two);
		assertThat(snapshot).containsOnlyKeys("one");
	}

	@Test
	public void getAllIsImmutable() {
		this.registry.register("one", this.one);
		Map<String, ReactiveHealthIndicator> snapshot = this.registry.getAll();
		assertThatExceptionOfType(UnsupportedOperationException.class)
				.isThrownBy(snapshot::clear);
	}

}
