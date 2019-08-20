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

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import org.springframework.boot.actuate.endpoint.SecurityContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Base class for {@link HealthEndpointSupport} tests.
 *
 * @param <R> the registry type
 * @param <C> the contributor type
 * @param <T> the contributed health component type
 * @author Phillip Webb
 */
abstract class HealthEndpointSupportTests<R extends ContributorRegistry<C>, C, T> {

	final R registry;

	final Health up = Health.up().withDetail("spring", "boot").build();

	final Health down = Health.down().build();

	final TestHealthEndpointSettings settings = new TestHealthEndpointSettings();

	HealthEndpointSupportTests() {
		this.registry = createRegistry();
	}

	@BeforeEach
	void setup() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	void createWhenRegistryIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> create(null, this.settings))
				.withMessage("Registry must not be null");
	}

	@Test
	void createWhenSettingsIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> create(this.registry, null))
				.withMessage("Settings must not be null");
	}

	@Test
	void getHealthWhenPathIsEmptyReturnsHealth() {
		this.registry.registerContributor("test", createContributor(this.up));
		T result = create(this.registry, this.settings).getHealth(SecurityContext.NONE, false);
		assertThat(getHealth(result)).isNotSameAs(this.up);
		assertThat(getHealth(result).getStatus()).isEqualTo(Status.UP);
	}

	@Test
	void getHealthWhenHasPathReturnsSubResult() {
		this.registry.registerContributor("test", createContributor(this.up));
		T result = create(this.registry, this.settings).getHealth(SecurityContext.NONE, false, "test");
		assertThat(getHealth(result)).isEqualTo(this.up);

	}

	@Test
	void getHealthWhenAlwaysIncludesDetailsIsFalseAndSettingsIsTrueIncludesDetails() {
		this.registry.registerContributor("test", createContributor(this.up));
		T result = create(this.registry, this.settings).getHealth(SecurityContext.NONE, false, "test");
		assertThat(((Health) getHealth(result)).getDetails()).containsEntry("spring", "boot");
	}

	@Test
	void getHealthWhenAlwaysIncludesDetailsIsFalseAndSettingsIsFalseIncludesNoDetails() {
		this.settings.setIncludeDetails(false);
		this.registry.registerContributor("test", createContributor(this.up));
		HealthEndpointSupport<C, T> endpoint = create(this.registry, this.settings);
		T rootResult = endpoint.getHealth(SecurityContext.NONE, false);
		T componentResult = endpoint.getHealth(SecurityContext.NONE, false, "test");
		assertThat(((CompositeHealth) getHealth(rootResult)).getStatus()).isEqualTo(Status.UP);
		assertThat(componentResult).isNull();
	}

	@Test
	void getHealthWhenAlwaysIncludesDetailsIsTrueIncludesDetails() {
		this.settings.setIncludeDetails(false);
		this.registry.registerContributor("test", createContributor(this.up));
		T result = create(this.registry, this.settings).getHealth(SecurityContext.NONE, true, "test");
		assertThat(((Health) getHealth(result)).getDetails()).containsEntry("spring", "boot");
	}

	@Test
	void getHealthWhenCompositeReturnsAggregateResult() {
		Map<String, C> contributors = new LinkedHashMap<>();
		contributors.put("a", createContributor(this.up));
		contributors.put("b", createContributor(this.down));
		this.registry.registerContributor("test", createCompositeContributor(contributors));
		T result = create(this.registry, this.settings).getHealth(SecurityContext.NONE, false);
		CompositeHealth root = (CompositeHealth) getHealth(result);
		CompositeHealth component = (CompositeHealth) root.getDetails().get("test");
		assertThat(root.getStatus()).isEqualTo(Status.DOWN);
		assertThat(component.getStatus()).isEqualTo(Status.DOWN);
		assertThat(component.getDetails()).containsOnlyKeys("a", "b");
	}

	@Test
	void getHealthWhenPathDoesNotExistReturnsNull() {
		T result = create(this.registry, this.settings).getHealth(SecurityContext.NONE, false, "missing");
		assertThat(result).isNull();
	}

	protected abstract HealthEndpointSupport<C, T> create(R registry, HealthEndpointSettings settings);

	protected abstract R createRegistry();

	protected abstract C createContributor(Health health);

	protected abstract C createCompositeContributor(Map<String, C> contributors);

	protected abstract HealthComponent getHealth(T result);

}
