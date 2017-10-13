/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.endpoint;

import java.util.Collections;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.boot.actuate.endpoint.EndpointDiscoverer;
import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.EndpointInfo;
import org.springframework.boot.actuate.endpoint.Operation;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ExposeExcludePropertyEndpointFilter}.
 *
 * @author Phillip Webb
 */
public class ExposeExcludePropertyEndpointFilterTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private MockEnvironment environment = new MockEnvironment();

	private EndpointFilter<Operation> filter;

	@Mock
	private TestEndpointDiscoverer discoverer;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void createWhenDiscovererTypeIsNullShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Discoverer Type must not be null");
		new ExposeExcludePropertyEndpointFilter<>(null, this.environment, "foo");
	}

	@Test
	public void createWhenEnvironmentIsNullShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Environment must not be null");
		new ExposeExcludePropertyEndpointFilter<>(TestEndpointDiscoverer.class, null,
				"foo");
	}

	@Test
	public void createWhenPrefixIsNullShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Prefix must not be empty");
		new ExposeExcludePropertyEndpointFilter<Operation>(TestEndpointDiscoverer.class,
				this.environment, null);
	}

	@Test
	public void createWhenPrefixIsEmptyShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Prefix must not be empty");
		new ExposeExcludePropertyEndpointFilter<Operation>(TestEndpointDiscoverer.class,
				this.environment, "");
	}

	@Test
	public void matchWhenExposeIsEmptyAndExcludeIsEmptyAndInDefaultShouldMatch()
			throws Exception {
		setupFilter("", "");
		assertThat(match("def")).isTrue();
	}

	@Test
	public void matchWhenExposeIsEmptyAndExcludeIsEmptyAndNotInDefaultShouldNotMatch()
			throws Exception {
		setupFilter("", "");
		assertThat(match("bar")).isFalse();
	}

	@Test
	public void matchWhenExposeMatchesAndExcludeIsEmptyShouldMatch() throws Exception {
		setupFilter("bar", "");
		assertThat(match("bar")).isTrue();
	}

	@Test
	public void matchWhenExposeDoesNotMatchAndExcludeIsEmptyShouldNotMatch()
			throws Exception {
		setupFilter("bar", "");
		assertThat(match("baz")).isFalse();
	}

	@Test
	public void matchWhenExposeMatchesAndExcludeMatchesShouldNotMatch() throws Exception {
		setupFilter("bar,baz", "baz");
		assertThat(match("baz")).isFalse();
	}

	@Test
	public void matchWhenExposeMatchesAndExcludeDoesNotMatchShouldMatch()
			throws Exception {
		setupFilter("bar,baz", "buz");
		assertThat(match("baz")).isTrue();
	}

	@Test
	public void matchWhenExposeMatchesWithDifferentCaseShouldMatch() throws Exception {
		setupFilter("bar", "");
		assertThat(match("bAr")).isTrue();
	}

	@Test
	public void matchWhenDicovererDoesNotMatchShouldMatch() throws Exception {
		this.environment.setProperty("foo.expose", "bar");
		this.environment.setProperty("foo.exclude", "");
		this.filter = new ExposeExcludePropertyEndpointFilter<>(
				DifferentTestEndpointDiscoverer.class, this.environment, "foo");
		assertThat(match("baz")).isTrue();
	}

	@Test
	public void matchWhenIncludeIsAsteriskShouldMatchAll() throws Exception {
		setupFilter("*", "buz");
		assertThat(match("bar")).isTrue();
		assertThat(match("baz")).isTrue();
		assertThat(match("buz")).isFalse();
	}

	@Test
	public void matchWhenExcludeIsAsteriskShouldMatchNone() throws Exception {
		setupFilter("bar,baz,buz", "*");
		assertThat(match("bar")).isFalse();
		assertThat(match("baz")).isFalse();
		assertThat(match("buz")).isFalse();
	}

	private void setupFilter(String expose, String exclude) {
		this.environment.setProperty("foo.expose", expose);
		this.environment.setProperty("foo.exclude", exclude);
		this.filter = new ExposeExcludePropertyEndpointFilter<>(
				TestEndpointDiscoverer.class, this.environment, "foo", "def");
	}

	private boolean match(String id) {
		EndpointInfo<Operation> info = new EndpointInfo<>(id, true,
				Collections.emptyList());
		return this.filter.match(info, this.discoverer);
	}

	private abstract static class TestEndpointDiscoverer
			implements EndpointDiscoverer<Operation> {

	}

	private abstract static class DifferentTestEndpointDiscoverer
			implements EndpointDiscoverer<Operation> {

	}

}
