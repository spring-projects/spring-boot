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

package org.springframework.boot.actuate.health;

import java.util.Collections;
import java.util.Iterator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DefaultContributorRegistry}.
 *
 * @author Phillip Webb
 * @author Vedran Pavic
 * @author Stephane Nicoll
 */
abstract class DefaultContributorRegistryTests {

	private final HealthIndicator one = mock(HealthIndicator.class);

	private final HealthIndicator two = mock(HealthIndicator.class);

	private ContributorRegistry<HealthIndicator> registry;

	@BeforeEach
	void setUp() {
		given(this.one.health()).willReturn(new Health.Builder().unknown().withDetail("1", "1").build());
		given(this.two.health()).willReturn(new Health.Builder().unknown().withDetail("2", "2").build());
		this.registry = new DefaultContributorRegistry<>();
	}

	@Test
	void createWhenContributorsIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new DefaultContributorRegistry<>(null))
				.withMessage("Contributors must not be null");
	}

	@Test
	void createWhenNameFactoryIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new DefaultContributorRegistry<>(Collections.emptyMap(), null))
				.withMessage("NameFactory must not be null");
	}

	@Test
	void createUsesHealthIndicatorNameFactoryByDefault() {
		this.registry = new DefaultContributorRegistry<>(Collections.singletonMap("oneHealthIndicator", this.one));
		assertThat(this.registry.getContributor("oneHealthIndicator")).isNull();
		assertThat(this.registry.getContributor("one")).isNotNull();
	}

	@Test
	void createWithCustomNameFactoryAppliesFunctionToName() {
		this.registry = new DefaultContributorRegistry<>(Collections.singletonMap("one", this.one), this::reverse);
		assertThat(this.registry.getContributor("one")).isNull();
		assertThat(this.registry.getContributor("eno")).isNotNull();
	}

	@Test
	void registerContributorWhenNameIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.registry.registerContributor(null, this.one))
				.withMessage("Name must not be null");
	}

	@Test
	void registerContributorWhenContributorIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.registry.registerContributor("one", null))
				.withMessage("Contributor must not be null");
	}

	@Test
	void registerContributorRegisteresContributors() {
		this.registry.registerContributor("one", this.one);
		this.registry.registerContributor("two", this.two);
		assertThat(this.registry).hasSize(2);
		assertThat(this.registry.getContributor("one")).isSameAs(this.one);
		assertThat(this.registry.getContributor("two")).isSameAs(this.two);
	}

	@Test
	void registerContributorWhenNameAlreadyUsedThrowsException() {
		this.registry.registerContributor("one", this.one);
		assertThatIllegalStateException().isThrownBy(() -> this.registry.registerContributor("one", this.two))
				.withMessageContaining("A contributor named \"one\" has already been registered");
	}

	@Test
	void registerContributorUsesNameFactory() {
		this.registry.registerContributor("oneHealthIndicator", this.one);
		assertThat(this.registry.getContributor("oneHealthIndicator")).isNull();
		assertThat(this.registry.getContributor("one")).isNotNull();
	}

	@Test
	void unregisterContributorUnregistersContributor() {
		this.registry.registerContributor("one", this.one);
		this.registry.registerContributor("two", this.two);
		assertThat(this.registry).hasSize(2);
		HealthIndicator two = this.registry.unregisterContributor("two");
		assertThat(two).isSameAs(this.two);
		assertThat(this.registry).hasSize(1);
	}

	@Test
	void unregisterContributorWhenUnknownReturnsNull() {
		this.registry.registerContributor("one", this.one);
		assertThat(this.registry).hasSize(1);
		HealthIndicator two = this.registry.unregisterContributor("two");
		assertThat(two).isNull();
		assertThat(this.registry).hasSize(1);
	}

	@Test
	void unregisterContributorUsesNameFactory() {
		this.registry.registerContributor("oneHealthIndicator", this.one);
		assertThat(this.registry.getContributor("oneHealthIndicator")).isNull();
		assertThat(this.registry.getContributor("one")).isNotNull();
	}

	@Test
	void getContributorReturnsContributor() {
		this.registry.registerContributor("one", this.one);
		assertThat(this.registry.getContributor("one")).isEqualTo(this.one);
	}

	@Test
	void iteratorIteratesContributors() {
		this.registry.registerContributor("one", this.one);
		this.registry.registerContributor("two", this.two);
		Iterator<NamedContributor<HealthIndicator>> iterator = this.registry.iterator();
		NamedContributor<HealthIndicator> first = iterator.next();
		NamedContributor<HealthIndicator> second = iterator.next();
		assertThat(iterator.hasNext()).isFalse();
		assertThat(first.getName()).isEqualTo("one");
		assertThat(first.getContributor()).isEqualTo(this.one);
		assertThat(second.getName()).isEqualTo("two");
		assertThat(second.getContributor()).isEqualTo(this.two);
	}

	private String reverse(String name) {
		return new StringBuilder(name).reverse().toString();
	}

}
