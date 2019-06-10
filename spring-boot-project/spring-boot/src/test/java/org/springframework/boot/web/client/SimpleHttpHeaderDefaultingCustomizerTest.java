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

package org.springframework.boot.web.client;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SimpleHttpHeaderDefaultingCustomizer}.
 *
 * @author Ilya Lukyanovich
 */
class SimpleHttpHeaderDefaultingCustomizerTest {

	@Test
	void testApplyTo_shouldAddAllHeaders() {
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.add("foo", "bar");
		httpHeaders.add("donut", "42");
		SimpleHttpHeaderDefaultingCustomizer customizer = new SimpleHttpHeaderDefaultingCustomizer(httpHeaders);
		HttpHeaders provided = new HttpHeaders();
		customizer.applyTo(provided);
		assertThat(provided).containsOnlyKeys("foo", "donut");
		assertThat(provided.get("foo")).containsExactly("bar");
		assertThat(provided.get("donut")).containsExactly("42");
	}

	@Test
	void testApplyTo_shouldIgnoreProvided() {
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.add("foo", "bar");
		httpHeaders.add("donut", "42");
		SimpleHttpHeaderDefaultingCustomizer customizer = new SimpleHttpHeaderDefaultingCustomizer(httpHeaders);
		HttpHeaders provided = new HttpHeaders();
		provided.add("donut", "touchme");
		customizer.applyTo(provided);
		assertThat(provided).containsOnlyKeys("foo", "donut");
		assertThat(provided.get("foo")).containsExactly("bar");
		assertThat(provided.get("donut")).containsExactly("touchme");
	}

	@Test
	void testSingleHeader() {
		HttpHeaders provided = new HttpHeaders();
		SimpleHttpHeaderDefaultingCustomizer.singleHeader("foo", "bar").applyTo(provided);
		assertThat(provided).containsOnlyKeys("foo");
		assertThat(provided.get("foo")).containsExactly("bar");
	}

	@Test
	void testBasicAuthentication() {
		HttpHeaders provided = new HttpHeaders();
		SimpleHttpHeaderDefaultingCustomizer.basicAuthentication("spring", "boot").applyTo(provided);
		assertThat(provided).containsOnlyKeys(HttpHeaders.AUTHORIZATION);
		assertThat(provided.get(HttpHeaders.AUTHORIZATION)).containsExactly("bar");
	}

}
