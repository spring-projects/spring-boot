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
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.http.ApiVersion;
import org.springframework.boot.actuate.health.HealthEndpointSupport.HealthResult;

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

	final TestHealthEndpointGroup primaryGroup = new TestHealthEndpointGroup();

	final TestHealthEndpointGroup allTheAs = new TestHealthEndpointGroup((name) -> name.startsWith("a"));

	final HealthEndpointGroups groups = HealthEndpointGroups.of(this.primaryGroup,
			Collections.singletonMap("alltheas", this.allTheAs));

	HealthEndpointSupportTests() {
		this.registry = createRegistry();
	}

	@BeforeEach
	void setup() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	void createWhenRegistryIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> create(null, this.groups))
				.withMessage("Registry must not be null");
	}

	@Test
	void createWhenGroupsIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> create(this.registry, null))
				.withMessage("Groups must not be null");
	}

	@Test
	void getHealthResultWhenPathIsEmptyUsesPrimaryGroup() {
		this.registry.registerContributor("test", createContributor(this.up));
		HealthResult<T> result = create(this.registry, this.groups).getHealth(ApiVersion.V3, SecurityContext.NONE,
				false);
		assertThat(result.getGroup()).isEqualTo(this.primaryGroup);
		assertThat(getHealth(result)).isNotSameAs(this.up);
		assertThat(getHealth(result).getStatus()).isEqualTo(Status.UP);
	}

	@Test
	void getHealthResultWhenPathIsNotGroupReturnsResultFromPrimaryGroup() {
		this.registry.registerContributor("test", createContributor(this.up));
		HealthResult<T> result = create(this.registry, this.groups).getHealth(ApiVersion.V3, SecurityContext.NONE,
				false, "test");
		assertThat(result.getGroup()).isEqualTo(this.primaryGroup);
		assertThat(getHealth(result)).isEqualTo(this.up);

	}

	@Test
	void getHealthResultWhenPathIsGroupReturnsResultFromGroup() {
		this.registry.registerContributor("atest", createContributor(this.up));
		HealthResult<T> result = create(this.registry, this.groups).getHealth(ApiVersion.V3, SecurityContext.NONE,
				false, "alltheas", "atest");
		assertThat(result.getGroup()).isEqualTo(this.allTheAs);
		assertThat(getHealth(result)).isEqualTo(this.up);
	}

	@Test
	void getHealthResultWhenAlwaysIncludesDetailsIsFalseAndGroupIsTrueIncludesDetails() {
		this.registry.registerContributor("test", createContributor(this.up));
		HealthResult<T> result = create(this.registry, this.groups).getHealth(ApiVersion.V3, SecurityContext.NONE,
				false, "test");
		assertThat(((Health) getHealth(result)).getDetails()).containsEntry("spring", "boot");
	}

	@Test
	void getHealthResultWhenAlwaysIncludesDetailsIsFalseAndGroupIsFalseIncludesNoDetails() {
		this.primaryGroup.setIncludeDetails(false);
		this.registry.registerContributor("test", createContributor(this.up));
		HealthEndpointSupport<C, T> endpoint = create(this.registry, this.groups);
		HealthResult<T> rootResult = endpoint.getHealth(ApiVersion.V3, SecurityContext.NONE, false);
		HealthResult<T> componentResult = endpoint.getHealth(ApiVersion.V3, SecurityContext.NONE, false, "test");
		assertThat(((CompositeHealth) getHealth(rootResult)).getStatus()).isEqualTo(Status.UP);
		assertThat(componentResult).isNull();
	}

	@Test
	void getHealthResultWhenAlwaysIncludesDetailsIsTrueIncludesDetails() {
		this.primaryGroup.setIncludeDetails(false);
		this.registry.registerContributor("test", createContributor(this.up));
		HealthResult<T> result = create(this.registry, this.groups).getHealth(ApiVersion.V3, SecurityContext.NONE, true,
				"test");
		assertThat(((Health) getHealth(result)).getDetails()).containsEntry("spring", "boot");
	}

	@Test
	void getHealthResultWhenCompositeReturnsAggregateResult() {
		Map<String, C> contributors = new LinkedHashMap<>();
		contributors.put("a", createContributor(this.up));
		contributors.put("b", createContributor(this.down));
		this.registry.registerContributor("test", createCompositeContributor(contributors));
		HealthResult<T> result = create(this.registry, this.groups).getHealth(ApiVersion.V3, SecurityContext.NONE,
				false);
		CompositeHealth root = (CompositeHealth) getHealth(result);
		CompositeHealth component = (CompositeHealth) root.getComponents().get("test");
		assertThat(root.getStatus()).isEqualTo(Status.DOWN);
		assertThat(component.getStatus()).isEqualTo(Status.DOWN);
		assertThat(component.getComponents()).containsOnlyKeys("a", "b");
	}

	@Test
	void getHealthResultWhenPathDoesNotExistReturnsNull() {
		HealthResult<T> result = create(this.registry, this.groups).getHealth(ApiVersion.V3, SecurityContext.NONE,
				false, "missing");
		assertThat(result).isNull();
	}

	@Test
	void getHealthResultWhenPathIsEmptyIncludesGroups() {
		this.registry.registerContributor("test", createContributor(this.up));
		HealthResult<T> result = create(this.registry, this.groups).getHealth(ApiVersion.V3, SecurityContext.NONE,
				false);
		assertThat(((SystemHealth) getHealth(result)).getGroups()).containsOnly("alltheas");
	}

	@Test
	void getHealthResultWhenPathIsGroupDoesNotIncludesGroups() {
		this.registry.registerContributor("atest", createContributor(this.up));
		HealthResult<T> result = create(this.registry, this.groups).getHealth(ApiVersion.V3, SecurityContext.NONE,
				false, "alltheas");
		assertThat(getHealth(result)).isNotInstanceOf(SystemHealth.class);
	}

	protected abstract HealthEndpointSupport<C, T> create(R registry, HealthEndpointGroups groups);

	protected abstract R createRegistry();

	protected abstract C createContributor(Health health);

	protected abstract C createCompositeContributor(Map<String, C> contributors);

	protected abstract HealthComponent getHealth(HealthResult<T> result);

}
