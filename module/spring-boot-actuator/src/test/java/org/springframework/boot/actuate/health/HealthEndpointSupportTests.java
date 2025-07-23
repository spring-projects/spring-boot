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

package org.springframework.boot.actuate.health;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.endpoint.ApiVersion;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.web.WebServerNamespace;
import org.springframework.boot.actuate.health.HealthEndpointSupport.Result;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for {@link HealthEndpointSupport} tests.
 *
 * @param <E> the endpoint type;
 * @param <H> the health type
 * @param <D> the descriptor type
 * @param <R> the registry type
 * @param <C> the contributor type
 * @author Phillip Webb
 * @author Madhura Bhave
 */
abstract class HealthEndpointSupportTests<E extends HealthEndpointSupport<H, D>, H, D, R, C> {

	final Health up = Health.up().withDetail("spring", "boot").build();

	final Health down = Health.down().build();

	final TestHealthEndpointGroup primaryGroup = new TestHealthEndpointGroup();

	final TestHealthEndpointGroup allTheAs = new TestHealthEndpointGroup((name) -> name.startsWith("a"));

	final HealthEndpointGroups groups = HealthEndpointGroups.of(this.primaryGroup, Map.of("alltheas", this.allTheAs));

	@Test
	void getResultWhenPathIsEmptyUsesPrimaryGroup() {
		R registry = createRegistry("test", createContributor(this.up));
		E support = create(registry, this.groups);
		Result<D> result = support.getResult(ApiVersion.V3, null, SecurityContext.NONE, false);
		assertThat(result.group()).isEqualTo(this.primaryGroup);
		SystemHealthDescriptor descriptor = (SystemHealthDescriptor) getDescriptor(result);
		assertThat(descriptor.getStatus()).isEqualTo(Status.UP);
		assertThat(descriptor.getComponents()).containsKey("test");
		assertThat(descriptor.getDetails()).isNull();
	}

	@Test
	void getResultWhenPathIsNotGroupReturnsResultFromPrimaryGroup() {
		R registry = createRegistry("test", createContributor(this.up));
		E endpoint = create(registry, this.groups);
		Result<D> result = endpoint.getResult(ApiVersion.V3, null, SecurityContext.NONE, false, "test");
		assertThat(result.group()).isEqualTo(this.primaryGroup);
		IndicatedHealthDescriptor descriptor = (IndicatedHealthDescriptor) getDescriptor(result);
		assertThat(descriptor.getStatus()).isEqualTo(Status.UP);
		assertThat(descriptor.getDetails()).containsEntry("spring", "boot");
	}

	@Test
	void getResultWhenPathIsGroupReturnsResultFromGroup() {
		R registry = createRegistry("atest", createContributor(this.up));
		E endpoint = create(registry, this.groups);
		Result<D> result = endpoint.getResult(ApiVersion.V3, null, SecurityContext.NONE, false, "alltheas", "atest");
		assertThat(result.group()).isEqualTo(this.allTheAs);
		IndicatedHealthDescriptor descriptor = (IndicatedHealthDescriptor) getDescriptor(result);
		assertThat(descriptor.getStatus()).isEqualTo(Status.UP);
		assertThat(descriptor.getDetails()).containsEntry("spring", "boot");
	}

	@Test
	void getResultWhenAlwaysShowIsFalseAndGroupIsTrueShowsComponents() {
		C contributor = createContributor(this.up);
		C compositeContributor = createCompositeContributor(Map.of("spring", contributor));
		R registry = createRegistry("test", compositeContributor);
		E endpoint = create(registry, this.groups);
		Result<D> result = endpoint.getResult(ApiVersion.V3, null, SecurityContext.NONE, false, "test");
		CompositeHealthDescriptor descriptor = (CompositeHealthDescriptor) getDescriptor(result);
		assertThat(descriptor.getComponents()).containsKey("spring");
	}

	@Test
	void getResultWhenAlwaysShowIsFalseAndGroupIsFalseCannotAccessComponent() {
		this.primaryGroup.setShowComponents(false);
		C contributor = createContributor(this.up);
		C compositeContributor = createCompositeContributor(Map.of("spring", contributor));
		R registry = createRegistry("test", compositeContributor);
		E endpoint = create(registry, this.groups);
		Result<D> rootResult = endpoint.getResult(ApiVersion.V3, null, SecurityContext.NONE, false);
		CompositeHealthDescriptor rootDescriptor = (CompositeHealthDescriptor) getDescriptor(rootResult);
		assertThat(rootDescriptor.getComponents()).isNullOrEmpty();
		Result<D> componentResult = endpoint.getResult(ApiVersion.V3, null, SecurityContext.NONE, false, "test");
		assertThat(componentResult).isNull();
	}

	@Test
	void getResultWhenAlwaysShowIsTrueShowsComponents() {
		this.primaryGroup.setShowComponents(true);
		C contributor = createContributor(this.up);
		C compositeContributor = createCompositeContributor(Map.of("spring", contributor));
		R registry = createRegistry("test", compositeContributor);
		E endpoint = create(registry, this.groups);
		Result<D> rootResult = endpoint.getResult(ApiVersion.V3, null, SecurityContext.NONE, false);
		CompositeHealthDescriptor rootDescriptor = (CompositeHealthDescriptor) getDescriptor(rootResult);
		assertThat(rootDescriptor.getComponents()).containsKey("test");
		Result<D> componentResult = endpoint.getResult(ApiVersion.V3, null, SecurityContext.NONE, false, "test");
		CompositeHealthDescriptor componentDescriptor = (CompositeHealthDescriptor) getDescriptor(componentResult);
		assertThat(componentDescriptor.getComponents()).containsKey("spring");
	}

	@Test
	void getResultWhenAlwaysShowIsFalseAndGroupIsTrueShowsDetails() {
		R registry = createRegistry("test", createContributor(this.up));
		E endpoint = create(registry, this.groups);
		Result<D> result = endpoint.getResult(ApiVersion.V3, null, SecurityContext.NONE, false, "test");
		IndicatedHealthDescriptor descriptor = (IndicatedHealthDescriptor) getDescriptor(result);
		assertThat(descriptor.getDetails()).containsEntry("spring", "boot");
	}

	@Test
	void getResultWhenAlwaysShowIsFalseAndGroupIsFalseShowsNoDetails() {
		this.primaryGroup.setShowDetails(false);
		R registry = createRegistry("test", createContributor(this.up));
		E endpoint = create(registry, this.groups);
		Result<D> rootResult = endpoint.getResult(ApiVersion.V3, null, SecurityContext.NONE, false);
		Result<D> componentResult = endpoint.getResult(ApiVersion.V3, null, SecurityContext.NONE, false, "test");
		assertThat(getDescriptor(rootResult).getStatus()).isEqualTo(Status.UP);
		assertThat(componentResult).isNull();
	}

	@Test
	void getResultWhenAlwaysShowIsTrueShowsDetails() {
		this.primaryGroup.setShowDetails(false);
		R registry = createRegistry("test", createContributor(this.up));
		E endpoint = create(registry, this.groups);
		Result<D> result = endpoint.getResult(ApiVersion.V3, null, SecurityContext.NONE, true, "test");
		IndicatedHealthDescriptor descriptor = (IndicatedHealthDescriptor) getDescriptor(result);
		assertThat(descriptor.getDetails()).containsEntry("spring", "boot");
	}

	@Test
	void getResultWhenCompositeReturnsAggregateResult() {
		Map<String, C> contributors = new LinkedHashMap<>();
		contributors.put("a", createContributor(this.up));
		contributors.put("b", createContributor(this.down));
		R registry = createRegistry("test", createCompositeContributor(contributors));
		E endpoint = create(registry, this.groups);
		Result<D> result = endpoint.getResult(ApiVersion.V3, null, SecurityContext.NONE, false);
		CompositeHealthDescriptor root = (CompositeHealthDescriptor) getDescriptor(result);
		CompositeHealthDescriptor component = (CompositeHealthDescriptor) root.getComponents().get("test");
		assertThat(root.getStatus()).isEqualTo(Status.DOWN);
		assertThat(component.getStatus()).isEqualTo(Status.DOWN);
		assertThat(component.getComponents()).containsOnlyKeys("a", "b");
	}

	@Test
	void getResultWhenPathDoesNotExistReturnsNull() {
		R registry = createRegistry("test", createCompositeContributor(Collections.emptyMap()));
		Result<D> result = create(registry, this.groups).getResult(ApiVersion.V3, null, SecurityContext.NONE, false,
				"missing");
		assertThat(result).isNull();
	}

	@Test
	void getResultWhenPathIsEmptyIncludesGroups() {
		R registry = createRegistry("test", createContributor(this.up));
		E endpoint = create(registry, this.groups);
		Result<D> result = endpoint.getResult(ApiVersion.V3, null, SecurityContext.NONE, false);
		SystemHealthDescriptor descriptor = (SystemHealthDescriptor) getDescriptor(result);
		assertThat(descriptor.getGroups()).containsOnly("alltheas");
	}

	@Test
	void getResultWhenPathIsGroupDoesNotIncludesGroups() {
		R registry = createRegistry("atest", createContributor(this.up));
		E endpoint = create(registry, this.groups);
		Result<D> result = endpoint.getResult(ApiVersion.V3, null, SecurityContext.NONE, false, "alltheas");
		HealthDescriptor descriptor = getDescriptor(result);
		assertThat(descriptor).isInstanceOf(CompositeHealthDescriptor.class);
		assertThat(descriptor).isNotInstanceOf(SystemHealthDescriptor.class);
	}

	@Test
	void getResultWithEmptyCompositeReturnsNullResult() { // gh-18687
		R registry = createRegistry("test", createCompositeContributor(Collections.emptyMap()));
		E endpoint = create(registry, this.groups);
		Result<D> result = endpoint.getResult(ApiVersion.V3, null, SecurityContext.NONE, false);
		assertThat(result).isNull();
	}

	@Test
	void getResultWhenGroupContainsCompositeContributorReturnsHealth() {
		C contributor = createContributor(this.up);
		C compositeContributor = createCompositeContributor(Map.of("spring", contributor));
		R registry = createRegistry("test", compositeContributor);
		TestHealthEndpointGroup testGroup = new TestHealthEndpointGroup((name) -> name.startsWith("test"));
		HealthEndpointGroups groups = HealthEndpointGroups.of(this.primaryGroup, Map.of("testGroup", testGroup));
		E endpoint = create(registry, groups);
		Result<D> result = endpoint.getResult(ApiVersion.V3, null, SecurityContext.NONE, false, "testGroup");
		CompositeHealthDescriptor descriptor = (CompositeHealthDescriptor) getDescriptor(result);
		assertThat(descriptor.getComponents()).containsKey("test");
	}

	@Test
	void getResultWhenGroupContainsComponentOfCompositeContributorReturnsHealth() {
		CompositeHealthDescriptor descriptor = getCompositeHealthDescriptor((name) -> name.equals("test/spring-1"));
		assertThat(descriptor.getComponents()).containsKey("test");
		CompositeHealthDescriptor test = (CompositeHealthDescriptor) descriptor.getComponents().get("test");
		assertThat(test.getComponents()).containsKey("spring-1");
		assertThat(test.getComponents()).doesNotContainKey("spring-2");
		assertThat(test.getComponents()).doesNotContainKey("test");
	}

	@Test
	void getResultWhenGroupExcludesComponentOfCompositeContributorReturnsHealth() {
		CompositeHealthDescriptor descriptor = getCompositeHealthDescriptor(
				(name) -> name.startsWith("test/") && !name.equals("test/spring-2"));
		assertThat(descriptor.getComponents()).containsKey("test");
		CompositeHealthDescriptor test = (CompositeHealthDescriptor) descriptor.getComponents().get("test");
		assertThat(test.getComponents()).containsKey("spring-1");
		assertThat(test.getComponents()).doesNotContainKey("spring-2");
	}

	private CompositeHealthDescriptor getCompositeHealthDescriptor(Predicate<String> memberPredicate) {
		C contributor1 = createContributor(this.up);
		C contributor2 = createContributor(this.down);
		Map<String, C> contributors = new LinkedHashMap<>();
		contributors.put("spring-1", contributor1);
		contributors.put("spring-2", contributor2);
		C compositeContributor = createCompositeContributor(contributors);
		R registry = createRegistry("test", compositeContributor);
		TestHealthEndpointGroup testGroup = new TestHealthEndpointGroup(memberPredicate);
		HealthEndpointGroups groups = HealthEndpointGroups.of(this.primaryGroup, Map.of("testGroup", testGroup));
		E endpoint = create(registry, groups);
		Result<D> result = endpoint.getResult(ApiVersion.V3, null, SecurityContext.NONE, false, "testGroup");
		return (CompositeHealthDescriptor) getDescriptor(result);
	}

	@Test
	void getResultForPathWhenGroupContainsComponentOfCompositeContributorReturnsHealth() {
		Map<String, C> contributors = new LinkedHashMap<>();
		contributors.put("spring-1", createNestedHealthContributor("spring-1"));
		contributors.put("spring-2", createNestedHealthContributor("spring-2"));
		C compositeContributor = createCompositeContributor(contributors);
		R registry = createRegistry("test", compositeContributor);
		TestHealthEndpointGroup testGroup = new TestHealthEndpointGroup(
				(name) -> name.startsWith("test") && !name.equals("test/spring-1/b"));
		HealthEndpointGroups groups = HealthEndpointGroups.of(this.primaryGroup, Map.of("testGroup", testGroup));
		E endpoint = create(registry, groups);
		Result<D> result = endpoint.getResult(ApiVersion.V3, null, SecurityContext.NONE, false, "testGroup", "test");
		CompositeHealthDescriptor descriptor = (CompositeHealthDescriptor) getDescriptor(result);
		assertThat(descriptor.getComponents()).containsKey("spring-1");
		assertThat(descriptor.getComponents()).containsKey("spring-2");
		CompositeHealthDescriptor spring1 = (CompositeHealthDescriptor) descriptor.getComponents().get("spring-1");
		CompositeHealthDescriptor spring2 = (CompositeHealthDescriptor) descriptor.getComponents().get("spring-2");
		assertThat(spring1.getComponents()).containsKey("a");
		assertThat(spring1.getComponents()).containsKey("c");
		assertThat(spring1.getComponents()).doesNotContainKey("b");
		assertThat(spring2.getComponents()).containsKey("a");
		assertThat(spring2.getComponents()).containsKey("c");
		assertThat(spring2.getComponents()).containsKey("b");
	}

	@Test
	void getResultForComponentPathWhenNotPartOfGroup() {
		Map<String, C> contributors = new LinkedHashMap<>();
		contributors.put("spring-1", createNestedHealthContributor("spring-1"));
		contributors.put("spring-2", createNestedHealthContributor("spring-2"));
		C compositeContributor = createCompositeContributor(contributors);
		R registry = createRegistry("test", compositeContributor);
		TestHealthEndpointGroup testGroup = new TestHealthEndpointGroup(
				(name) -> name.startsWith("test") && !name.equals("test/spring-1/b"));
		HealthEndpointGroups groups = HealthEndpointGroups.of(this.primaryGroup, Map.of("testGroup", testGroup));
		E endpoint = create(registry, groups);
		Result<D> result = endpoint.getResult(ApiVersion.V3, null, SecurityContext.NONE, false, "testGroup", "test",
				"spring-1", "b");
		assertThat(result).isNull();
	}

	private C createNestedHealthContributor(String name) {
		Map<String, C> map = new LinkedHashMap<>();
		map.put("a", createContributor(Health.up().withDetail("hello", name + "-a").build()));
		map.put("b", createContributor(Health.up().withDetail("hello", name + "-b").build()));
		map.put("c", createContributor(Health.up().withDetail("hello", name + "-c").build()));
		return createCompositeContributor(map);
	}

	@Test
	void getResultWhenGroupHasAdditionalPath() {
		R registry = createRegistry("test", createContributor(this.up));
		TestHealthEndpointGroup testGroup = new TestHealthEndpointGroup((name) -> name.startsWith("test"));
		testGroup.setAdditionalPath(AdditionalHealthEndpointPath.from("server:/healthz"));
		HealthEndpointGroups groups = HealthEndpointGroups.of(this.primaryGroup, Map.of("testGroup", testGroup));
		E endpoint = create(registry, groups);
		Result<D> result = endpoint.getResult(ApiVersion.V3, WebServerNamespace.SERVER, SecurityContext.NONE, false,
				"healthz");
		CompositeHealthDescriptor descriptor = (CompositeHealthDescriptor) getDescriptor(result);
		assertThat(descriptor.getComponents()).containsKey("test");
	}

	@Test
	void getResultWhenGroupHasAdditionalPathAndShowComponentsFalse() {
		R registry = createRegistry("test", createContributor(this.up));
		TestHealthEndpointGroup testGroup = new TestHealthEndpointGroup((name) -> name.startsWith("test"));
		testGroup.setAdditionalPath(AdditionalHealthEndpointPath.from("server:/healthz"));
		testGroup.setShowComponents(false);
		HealthEndpointGroups groups = HealthEndpointGroups.of(this.primaryGroup, Map.of("testGroup", testGroup));
		E endpoint = create(registry, groups);
		Result<D> result = endpoint.getResult(ApiVersion.V3, WebServerNamespace.SERVER, SecurityContext.NONE, false,
				"healthz");
		CompositeHealthDescriptor descriptor = (CompositeHealthDescriptor) getDescriptor(result);
		assertThat(descriptor.getStatus().getCode()).isEqualTo("UP");
		assertThat(descriptor.getComponents()).isNull();
	}

	@Test
	void getResultWithPathWhenGroupHasAdditionalPathAndShowComponentsFalse() {
		R registry = createRegistry("test", createContributor(this.up));
		TestHealthEndpointGroup testGroup = new TestHealthEndpointGroup((name) -> name.startsWith("test"));
		testGroup.setAdditionalPath(AdditionalHealthEndpointPath.from("server:/healthz"));
		testGroup.setShowComponents(false);
		HealthEndpointGroups groups = HealthEndpointGroups.of(this.primaryGroup, Map.of("testGroup", testGroup));
		E endpoint = create(registry, groups);
		Result<D> result = endpoint.getResult(ApiVersion.V3, WebServerNamespace.SERVER, SecurityContext.NONE, false,
				"healthz", "test");
		assertThat(result).isNull();
	}

	protected final E create(R registry, HealthEndpointGroups groups) {
		return create(registry, groups, null);
	}

	protected abstract E create(R registry, HealthEndpointGroups groups, Duration slowContributorLoggingThreshold);

	protected final R createRegistry(String name, C contributor) {
		return createRegistry((registrations) -> registrations.accept(name, contributor));
	}

	protected abstract R createRegistry(Consumer<BiConsumer<String, C>> initialRegistrations);

	protected abstract C createContributor(Health health);

	protected abstract C createCompositeContributor(Map<String, C> contributors);

	protected abstract HealthDescriptor getDescriptor(Result<D> result);

}
