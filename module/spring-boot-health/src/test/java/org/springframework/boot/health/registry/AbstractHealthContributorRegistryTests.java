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
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.util.Assert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link AbstractRegistry}.
 *
 * @param <C> the contributor type
 * @param <E> the entry type
 * @author Phillip Webb
 * @author Vedran Pavic
 * @author Stephane Nicoll
 */
abstract class AbstractHealthContributorRegistryTests<C, E> {

	private AbstractRegistry<C, E> registry;

	@BeforeEach
	void setUp() {
		this.registry = createRegistry(Collections.emptyList(), null);
	}

	@Test
	void registerContributorWhenNameIsNullThrowsException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.registry.registerContributor(null, mockHealthIndicator()))
			.withMessage("'name' must not be empty");
	}

	@Test
	void registerContributorWhenContributorIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.registry.registerContributor("one", null))
			.withMessage("'contributor' must not be null");
	}

	@Test
	void registerContributorRegistersContributors() {
		C c1 = mockHealthIndicator();
		C c2 = mockHealthIndicator();
		this.registry.registerContributor("one", c1);
		this.registry.registerContributor("two", c2);
		assertThat((Iterable<?>) this.registry).hasSize(2);
		assertThat(this.registry.getContributor("one")).isSameAs(c1);
		assertThat(this.registry.getContributor("two")).isSameAs(c2);
	}

	@Test
	void registerContributorWhenNameAlreadyUsedThrowsException() {
		this.registry.registerContributor("one", mockHealthIndicator());
		assertThatIllegalStateException()
			.isThrownBy(() -> this.registry.registerContributor("one", mockHealthIndicator()))
			.withMessageContaining("A contributor named \"one\" has already been registered");
	}

	@Test
	void unregisterContributorUnregistersContributor() {
		C c1 = mockHealthIndicator();
		C c2 = mockHealthIndicator();
		this.registry.registerContributor("one", c1);
		this.registry.registerContributor("two", c2);
		assertThat((Iterable<?>) this.registry).hasSize(2);
		C two = this.registry.unregisterContributor("two");
		assertThat(two).isSameAs(c2);
		assertThat((Iterable<?>) this.registry).hasSize(1);
	}

	@Test
	void unregisterContributorWhenUnknownReturnsNull() {
		this.registry.registerContributor("one", mockHealthIndicator());
		assertThat((Iterable<?>) this.registry).hasSize(1);
		Object two = this.registry.unregisterContributor("two");
		assertThat(two).isNull();
		assertThat((Iterable<?>) this.registry).hasSize(1);
	}

	@Test
	void getContributorReturnsContributor() {
		C c1 = mockHealthIndicator();
		this.registry.registerContributor("one", c1);
		assertThat(this.registry.getContributor("one")).isEqualTo(c1);
	}

	@Test
	void streamStreamsContributors() {
		C c1 = mockHealthIndicator();
		C c2 = mockHealthIndicator();
		this.registry.registerContributor("one", c1);
		this.registry.registerContributor("two", c2);
		List<E> streamed = this.registry.stream().toList();
		assertThat(streamed).hasSize(2);
		E first = streamed.get(0);
		E second = streamed.get(1);
		assertThat(name(first)).isEqualTo("one");
		assertThat(contributor(first)).isEqualTo(c1);
		assertThat(name(second)).isEqualTo("two");
		assertThat(contributor(second)).isEqualTo(c2);
	}

	@Test
	void nameValidatorsValidateMapKeys() {
		assertThatIllegalStateException().isThrownBy(() -> createRegistry(testValidator(), (initialRegistrations) -> {
			initialRegistrations.accept("ok", mockHealthIndicator());
			initialRegistrations.accept("fail", mockHealthIndicator());
		})).withMessage("Failed validation");
	}

	@Test
	void nameValidatorsValidateRegisteredName() {
		AbstractRegistry<C, E> registry = createRegistry(testValidator(), null);
		registry.registerContributor("ok", mockHealthIndicator());
		assertThatIllegalStateException().isThrownBy(() -> registry.registerContributor("fail", mockHealthIndicator()))
			.withMessage("Failed validation");
	}

	private List<HealthContributorNameValidator> testValidator() {
		return List.of((name) -> Assert.state(!"fail".equals(name), "Failed validation"));
	}

	protected abstract AbstractRegistry<C, E> createRegistry(
			Collection<? extends HealthContributorNameValidator> nameValidators,
			Consumer<BiConsumer<String, C>> initialRegistrations);

	protected abstract C mockHealthIndicator();

	protected abstract String name(E entry);

	protected abstract C contributor(E entry);

}
