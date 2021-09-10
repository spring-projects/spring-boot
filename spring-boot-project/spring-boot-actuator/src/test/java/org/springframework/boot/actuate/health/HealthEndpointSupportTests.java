/*
 * Copyright 2012-2021 the original author or authors.
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
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.endpoint.ApiVersion;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.web.WebServerNamespace;
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
 * @author Madhura Bhave
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
	void getHealthWhenPathIsEmptyUsesPrimaryGroup() {
		this.registry.registerContributor("test", createContributor(this.up));
		HealthResult<T> result = create(this.registry, this.groups).getHealth(ApiVersion.V3, null, SecurityContext.NONE,
				false);
		assertThat(result.getGroup()).isEqualTo(this.primaryGroup);
		assertThat(getHealth(result)).isNotSameAs(this.up);
		assertThat(getHealth(result).getStatus()).isEqualTo(Status.UP);
	}

	@Test
	void getHealthWhenPathIsNotGroupReturnsResultFromPrimaryGroup() {
		this.registry.registerContributor("test", createContributor(this.up));
		HealthResult<T> result = create(this.registry, this.groups).getHealth(ApiVersion.V3, null, SecurityContext.NONE,
				false, "test");
		assertThat(result.getGroup()).isEqualTo(this.primaryGroup);
		assertThat(getHealth(result)).isEqualTo(this.up);

	}

	@Test
	void getHealthWhenPathIsGroupReturnsResultFromGroup() {
		this.registry.registerContributor("atest", createContributor(this.up));
		HealthResult<T> result = create(this.registry, this.groups).getHealth(ApiVersion.V3, null, SecurityContext.NONE,
				false, "alltheas", "atest");
		assertThat(result.getGroup()).isEqualTo(this.allTheAs);
		assertThat(getHealth(result)).isEqualTo(this.up);
	}

	@Test
	void getHealthWhenAlwaysShowIsFalseAndGroupIsTrueShowsComponents() {
		C contributor = createContributor(this.up);
		C compositeContributor = createCompositeContributor(Collections.singletonMap("spring", contributor));
		this.registry.registerContributor("test", compositeContributor);
		HealthResult<T> result = create(this.registry, this.groups).getHealth(ApiVersion.V3, null, SecurityContext.NONE,
				false, "test");
		CompositeHealth health = (CompositeHealth) getHealth(result);
		assertThat(health.getComponents()).containsKey("spring");
	}

	@Test
	void getHealthWhenAlwaysShowIsFalseAndGroupIsFalseCannotAccessComponent() {
		this.primaryGroup.setShowComponents(false);
		C contributor = createContributor(this.up);
		C compositeContributor = createCompositeContributor(Collections.singletonMap("spring", contributor));
		this.registry.registerContributor("test", compositeContributor);
		HealthEndpointSupport<C, T> endpoint = create(this.registry, this.groups);
		HealthResult<T> rootResult = endpoint.getHealth(ApiVersion.V3, null, SecurityContext.NONE, false);
		assertThat(((CompositeHealth) getHealth(rootResult)).getComponents()).isNullOrEmpty();
		HealthResult<T> componentResult = endpoint.getHealth(ApiVersion.V3, null, SecurityContext.NONE, false, "test");
		assertThat(componentResult).isNull();
	}

	@Test
	void getHealthWhenAlwaysShowIsTrueShowsComponents() {
		this.primaryGroup.setShowComponents(true);
		C contributor = createContributor(this.up);
		C compositeContributor = createCompositeContributor(Collections.singletonMap("spring", contributor));
		this.registry.registerContributor("test", compositeContributor);
		HealthEndpointSupport<C, T> endpoint = create(this.registry, this.groups);
		HealthResult<T> rootResult = endpoint.getHealth(ApiVersion.V3, null, SecurityContext.NONE, false);
		assertThat(((CompositeHealth) getHealth(rootResult)).getComponents()).containsKey("test");
		HealthResult<T> componentResult = endpoint.getHealth(ApiVersion.V3, null, SecurityContext.NONE, false, "test");
		assertThat(((CompositeHealth) getHealth(componentResult)).getComponents()).containsKey("spring");
	}

	@Test
	void getHealthWhenAlwaysShowIsFalseAndGroupIsTrueShowsDetails() {
		this.registry.registerContributor("test", createContributor(this.up));
		HealthResult<T> result = create(this.registry, this.groups).getHealth(ApiVersion.V3, null, SecurityContext.NONE,
				false, "test");
		assertThat(((Health) getHealth(result)).getDetails()).containsEntry("spring", "boot");
	}

	@Test
	void getHealthWhenAlwaysShowIsFalseAndGroupIsFalseShowsNoDetails() {
		this.primaryGroup.setShowDetails(false);
		this.registry.registerContributor("test", createContributor(this.up));
		HealthEndpointSupport<C, T> endpoint = create(this.registry, this.groups);
		HealthResult<T> rootResult = endpoint.getHealth(ApiVersion.V3, null, SecurityContext.NONE, false);
		HealthResult<T> componentResult = endpoint.getHealth(ApiVersion.V3, null, SecurityContext.NONE, false, "test");
		assertThat(((CompositeHealth) getHealth(rootResult)).getStatus()).isEqualTo(Status.UP);
		assertThat(componentResult).isNull();
	}

	@Test
	void getHealthWhenAlwaysShowIsTrueShowsDetails() {
		this.primaryGroup.setShowDetails(false);
		this.registry.registerContributor("test", createContributor(this.up));
		HealthResult<T> result = create(this.registry, this.groups).getHealth(ApiVersion.V3, null, SecurityContext.NONE,
				true, "test");
		assertThat(((Health) getHealth(result)).getDetails()).containsEntry("spring", "boot");
	}

	@Test
	void getHealthWhenCompositeReturnsAggregateResult() {
		Map<String, C> contributors = new LinkedHashMap<>();
		contributors.put("a", createContributor(this.up));
		contributors.put("b", createContributor(this.down));
		this.registry.registerContributor("test", createCompositeContributor(contributors));
		HealthResult<T> result = create(this.registry, this.groups).getHealth(ApiVersion.V3, null, SecurityContext.NONE,
				false);
		CompositeHealth root = (CompositeHealth) getHealth(result);
		CompositeHealth component = (CompositeHealth) root.getComponents().get("test");
		assertThat(root.getStatus()).isEqualTo(Status.DOWN);
		assertThat(component.getStatus()).isEqualTo(Status.DOWN);
		assertThat(component.getComponents()).containsOnlyKeys("a", "b");
	}

	@Test
	void getHealthWhenPathDoesNotExistReturnsNull() {
		HealthResult<T> result = create(this.registry, this.groups).getHealth(ApiVersion.V3, null, SecurityContext.NONE,
				false, "missing");
		assertThat(result).isNull();
	}

	@Test
	void getHealthWhenPathIsEmptyIncludesGroups() {
		this.registry.registerContributor("test", createContributor(this.up));
		HealthResult<T> result = create(this.registry, this.groups).getHealth(ApiVersion.V3, null, SecurityContext.NONE,
				false);
		assertThat(((SystemHealth) getHealth(result)).getGroups()).containsOnly("alltheas");
	}

	@Test
	void getHealthWhenPathIsGroupDoesNotIncludesGroups() {
		this.registry.registerContributor("atest", createContributor(this.up));
		HealthResult<T> result = create(this.registry, this.groups).getHealth(ApiVersion.V3, null, SecurityContext.NONE,
				false, "alltheas");
		assertThat(getHealth(result)).isNotInstanceOf(SystemHealth.class);
	}

	@Test
	void getHealthWithEmptyCompositeReturnsNullResult() { // gh-18687
		this.registry.registerContributor("test", createCompositeContributor(Collections.emptyMap()));
		HealthResult<T> result = create(this.registry, this.groups).getHealth(ApiVersion.V3, null, SecurityContext.NONE,
				false);
		assertThat(result).isNull();
	}

	@Test
	void getHealthWhenGroupContainsCompositeContributorReturnsHealth() {
		C contributor = createContributor(this.up);
		C compositeContributor = createCompositeContributor(Collections.singletonMap("spring", contributor));
		this.registry.registerContributor("test", compositeContributor);
		TestHealthEndpointGroup testGroup = new TestHealthEndpointGroup((name) -> name.startsWith("test"));
		HealthEndpointGroups groups = HealthEndpointGroups.of(this.primaryGroup,
				Collections.singletonMap("testGroup", testGroup));
		HealthResult<T> result = create(this.registry, groups).getHealth(ApiVersion.V3, null, SecurityContext.NONE,
				false, "testGroup");
		CompositeHealth health = (CompositeHealth) getHealth(result);
		assertThat(health.getComponents()).containsKey("test");
	}

	@Test
	void getHealthWhenGroupContainsComponentOfCompositeContributorReturnsHealth() {
		CompositeHealth health = getCompositeHealth((name) -> name.equals("test/spring-1"));
		assertThat(health.getComponents()).containsKey("test");
		CompositeHealth test = (CompositeHealth) health.getComponents().get("test");
		assertThat(test.getComponents()).containsKey("spring-1");
		assertThat(test.getComponents()).doesNotContainKey("spring-2");
		assertThat(test.getComponents()).doesNotContainKey("test");
	}

	@Test
	void getHealthWhenGroupExcludesComponentOfCompositeContributorReturnsHealth() {
		CompositeHealth health = getCompositeHealth(
				(name) -> name.startsWith("test/") && !name.equals("test/spring-2"));
		assertThat(health.getComponents()).containsKey("test");
		CompositeHealth test = (CompositeHealth) health.getComponents().get("test");
		assertThat(test.getComponents()).containsKey("spring-1");
		assertThat(test.getComponents()).doesNotContainKey("spring-2");
	}

	@Test
	void getHealthForPathWhenGroupContainsComponentOfCompositeContributorReturnsHealth() {
		Map<String, C> contributors = new LinkedHashMap<>();
		contributors.put("spring-1", createNestedHealthContributor("spring-1"));
		contributors.put("spring-2", createNestedHealthContributor("spring-2"));
		C compositeContributor = createCompositeContributor(contributors);
		this.registry.registerContributor("test", compositeContributor);
		TestHealthEndpointGroup testGroup = new TestHealthEndpointGroup(
				(name) -> name.startsWith("test") && !name.equals("test/spring-1/b"));
		HealthEndpointGroups groups = HealthEndpointGroups.of(this.primaryGroup,
				Collections.singletonMap("testGroup", testGroup));
		HealthResult<T> result = create(this.registry, groups).getHealth(ApiVersion.V3, null, SecurityContext.NONE,
				false, "testGroup", "test");
		CompositeHealth health = (CompositeHealth) getHealth(result);
		assertThat(health.getComponents()).containsKey("spring-1");
		assertThat(health.getComponents()).containsKey("spring-2");
		CompositeHealth spring1 = (CompositeHealth) health.getComponents().get("spring-1");
		CompositeHealth spring2 = (CompositeHealth) health.getComponents().get("spring-2");
		assertThat(spring1.getComponents()).containsKey("a");
		assertThat(spring1.getComponents()).containsKey("c");
		assertThat(spring1.getComponents()).doesNotContainKey("b");
		assertThat(spring2.getComponents()).containsKey("a");
		assertThat(spring2.getComponents()).containsKey("c");
		assertThat(spring2.getComponents()).containsKey("b");
	}

	@Test
	void getHealthForComponentPathWhenNotPartOfGroup() {
		Map<String, C> contributors = new LinkedHashMap<>();
		contributors.put("spring-1", createNestedHealthContributor("spring-1"));
		contributors.put("spring-2", createNestedHealthContributor("spring-2"));
		C compositeContributor = createCompositeContributor(contributors);
		this.registry.registerContributor("test", compositeContributor);
		TestHealthEndpointGroup testGroup = new TestHealthEndpointGroup(
				(name) -> name.startsWith("test") && !name.equals("test/spring-1/b"));
		HealthEndpointGroups groups = HealthEndpointGroups.of(this.primaryGroup,
				Collections.singletonMap("testGroup", testGroup));
		HealthResult<T> result = create(this.registry, groups).getHealth(ApiVersion.V3, null, SecurityContext.NONE,
				false, "testGroup", "test", "spring-1", "b");
		assertThat(result).isNull();
	}

	private CompositeHealth getCompositeHealth(Predicate<String> memberPredicate) {
		C contributor1 = createContributor(this.up);
		C contributor2 = createContributor(this.down);
		Map<String, C> contributors = new LinkedHashMap<>();
		contributors.put("spring-1", contributor1);
		contributors.put("spring-2", contributor2);
		C compositeContributor = createCompositeContributor(contributors);
		this.registry.registerContributor("test", compositeContributor);
		TestHealthEndpointGroup testGroup = new TestHealthEndpointGroup(memberPredicate);
		HealthEndpointGroups groups = HealthEndpointGroups.of(this.primaryGroup,
				Collections.singletonMap("testGroup", testGroup));
		HealthResult<T> result = create(this.registry, groups).getHealth(ApiVersion.V3, null, SecurityContext.NONE,
				false, "testGroup");
		return (CompositeHealth) getHealth(result);
	}

	private C createNestedHealthContributor(String name) {
		Map<String, C> map = new LinkedHashMap<>();
		map.put("a", createContributor(Health.up().withDetail("hello", name + "-a").build()));
		map.put("b", createContributor(Health.up().withDetail("hello", name + "-b").build()));
		map.put("c", createContributor(Health.up().withDetail("hello", name + "-c").build()));
		return createCompositeContributor(map);
	}

	@Test
	void getHealthWhenGroupHasAdditionalPath() {
		this.registry.registerContributor("test", createContributor(this.up));
		TestHealthEndpointGroup testGroup = new TestHealthEndpointGroup((name) -> name.startsWith("test"));
		testGroup.setAdditionalPath(AdditionalHealthEndpointPath.from("server:/healthz"));
		HealthEndpointGroups groups = HealthEndpointGroups.of(this.primaryGroup,
				Collections.singletonMap("testGroup", testGroup));
		HealthResult<T> result = create(this.registry, groups).getHealth(ApiVersion.V3, WebServerNamespace.SERVER,
				SecurityContext.NONE, false, "healthz");
		CompositeHealth health = (CompositeHealth) getHealth(result);
		assertThat(health.getComponents()).containsKey("test");
	}

	@Test
	void getHealthWhenGroupHasAdditionalPathAndShowComponentsFalse() {
		this.registry.registerContributor("test", createContributor(this.up));
		TestHealthEndpointGroup testGroup = new TestHealthEndpointGroup((name) -> name.startsWith("test"));
		testGroup.setAdditionalPath(AdditionalHealthEndpointPath.from("server:/healthz"));
		testGroup.setShowComponents(false);
		HealthEndpointGroups groups = HealthEndpointGroups.of(this.primaryGroup,
				Collections.singletonMap("testGroup", testGroup));
		HealthResult<T> result = create(this.registry, groups).getHealth(ApiVersion.V3, WebServerNamespace.SERVER,
				SecurityContext.NONE, false, "healthz");
		CompositeHealth health = (CompositeHealth) getHealth(result);
		assertThat(health.getStatus().getCode()).isEqualTo("UP");
		assertThat(health.getComponents()).isNull();
	}

	@Test
	void getComponentHealthWhenGroupHasAdditionalPathAndShowComponentsFalse() {
		this.registry.registerContributor("test", createContributor(this.up));
		TestHealthEndpointGroup testGroup = new TestHealthEndpointGroup((name) -> name.startsWith("test"));
		testGroup.setAdditionalPath(AdditionalHealthEndpointPath.from("server:/healthz"));
		testGroup.setShowComponents(false);
		HealthEndpointGroups groups = HealthEndpointGroups.of(this.primaryGroup,
				Collections.singletonMap("testGroup", testGroup));
		HealthResult<T> result = create(this.registry, groups).getHealth(ApiVersion.V3, WebServerNamespace.SERVER,
				SecurityContext.NONE, false, "healthz", "test");
		assertThat(result).isEqualTo(null);
	}

	protected abstract HealthEndpointSupport<C, T> create(R registry, HealthEndpointGroups groups);

	protected abstract R createRegistry();

	protected abstract C createContributor(Health health);

	protected abstract C createCompositeContributor(Map<String, C> contributors);

	protected abstract HealthComponent getHealth(HealthResult<T> result);

}
