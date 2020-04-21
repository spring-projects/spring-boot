/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.endpoint.expose;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link IncludeExcludeEndpointFilter}.
 *
 * @author Phillip Webb
 */
class IncludeExcludeEndpointFilterTests {

	private IncludeExcludeEndpointFilter<?> filter;

	@BeforeEach
	void setup() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	void createWhenEndpointTypeIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new IncludeExcludeEndpointFilter<>(null, new MockEnvironment(), "foo"))
				.withMessageContaining("EndpointType must not be null");
	}

	@Test
	void createWhenEnvironmentIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new IncludeExcludeEndpointFilter<>(ExposableEndpoint.class, null, "foo"))
				.withMessageContaining("Environment must not be null");
	}

	@Test
	void createWhenPrefixIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(
						() -> new IncludeExcludeEndpointFilter<>(ExposableEndpoint.class, new MockEnvironment(), null))
				.withMessageContaining("Prefix must not be empty");
	}

	@Test
	void createWhenPrefixIsEmptyShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(
						() -> new IncludeExcludeEndpointFilter<>(ExposableEndpoint.class, new MockEnvironment(), ""))
				.withMessageContaining("Prefix must not be empty");
	}

	@Test
	void matchWhenExposeIsEmptyAndExcludeIsEmptyAndInDefaultShouldMatch() {
		setupFilter("", "");
		assertThat(match(EndpointId.of("def"))).isTrue();
	}

	@Test
	void matchWhenExposeIsEmptyAndExcludeIsEmptyAndNotInDefaultShouldNotMatch() {
		setupFilter("", "");
		assertThat(match(EndpointId.of("bar"))).isFalse();
	}

	@Test
	void matchWhenExposeMatchesAndExcludeIsEmptyShouldMatch() {
		setupFilter("bar", "");
		assertThat(match(EndpointId.of("bar"))).isTrue();
	}

	@Test
	void matchWhenExposeDoesNotMatchAndExcludeIsEmptyShouldNotMatch() {
		setupFilter("bar", "");
		assertThat(match(EndpointId.of("baz"))).isFalse();
	}

	@Test
	void matchWhenExposeMatchesAndExcludeMatchesShouldNotMatch() {
		setupFilter("bar,baz", "baz");
		assertThat(match(EndpointId.of("baz"))).isFalse();
	}

	@Test
	void matchWhenExposeMatchesAndExcludeDoesNotMatchShouldMatch() {
		setupFilter("bar,baz", "buz");
		assertThat(match(EndpointId.of("baz"))).isTrue();
	}

	@Test
	void matchWhenExposeMatchesWithDifferentCaseShouldMatch() {
		setupFilter("bar", "");
		assertThat(match(EndpointId.of("bAr"))).isTrue();
	}

	@Test
	void matchWhenDiscovererDoesNotMatchShouldMatch() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("foo.include", "bar");
		environment.setProperty("foo.exclude", "");
		this.filter = new IncludeExcludeEndpointFilter<>(DifferentTestExposableWebEndpoint.class, environment, "foo");
		assertThat(match(EndpointId.of("baz"))).isTrue();
	}

	@Test
	void matchWhenIncludeIsAsteriskShouldMatchAll() {
		setupFilter("*", "buz");
		assertThat(match(EndpointId.of("bar"))).isTrue();
		assertThat(match(EndpointId.of("baz"))).isTrue();
		assertThat(match(EndpointId.of("buz"))).isFalse();
	}

	@Test
	void matchWhenExcludeIsAsteriskShouldMatchNone() {
		setupFilter("bar,baz,buz", "*");
		assertThat(match(EndpointId.of("bar"))).isFalse();
		assertThat(match(EndpointId.of("baz"))).isFalse();
		assertThat(match(EndpointId.of("buz"))).isFalse();
	}

	@Test
	void matchWhenMixedCaseShouldMatch() {
		setupFilter("foo-bar", "");
		assertThat(match(EndpointId.of("fooBar"))).isTrue();
	}

	@Test // gh-20997
	void matchWhenDashInName() throws Exception {
		setupFilter("bus-refresh", "");
		assertThat(match(EndpointId.of("bus-refresh"))).isTrue();
	}

	private void setupFilter(String include, String exclude) {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("foo.include", include);
		environment.setProperty("foo.exclude", exclude);
		this.filter = new IncludeExcludeEndpointFilter<>(TestExposableWebEndpoint.class, environment, "foo", "def");
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private boolean match(EndpointId id) {
		ExposableEndpoint<?> endpoint = mock(TestExposableWebEndpoint.class);
		given(endpoint.getEndpointId()).willReturn(id);
		return ((EndpointFilter) this.filter).match(endpoint);
	}

	abstract static class TestExposableWebEndpoint implements ExposableWebEndpoint {

	}

	abstract static class DifferentTestExposableWebEndpoint implements ExposableWebEndpoint {

	}

}
