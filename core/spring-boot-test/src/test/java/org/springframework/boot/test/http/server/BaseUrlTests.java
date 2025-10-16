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

package org.springframework.boot.test.http.server;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link BaseUrl}.
 *
 * @author Phillip Webb
 */
class BaseUrlTests {

	@Test
	void resolveWithString() {
		assertThat(BaseUrl.of("http://localhost").resolve(null)).isEqualTo("http://localhost");
		assertThat(BaseUrl.of("http://localhost").resolve("")).isEqualTo("http://localhost");
		assertThat(BaseUrl.of("http://localhost").resolve("path")).isEqualTo("http://localhost/path");
		assertThat(BaseUrl.of("http://localhost").resolve("/path")).isEqualTo("http://localhost/path");
		assertThat(BaseUrl.of("http://localhost/").resolve("path")).isEqualTo("http://localhost/path");
		assertThat(BaseUrl.of("http://localhost/").resolve("/path")).isEqualTo("http://localhost/path");
	}

	@Test
	void ofWhenHttp() {
		BaseUrl baseUrl = BaseUrl.of("http://localhost:8080/context");
		assertThat(baseUrl.isHttps()).isFalse();
		assertThat(baseUrl.resolve()).isEqualTo("http://localhost:8080/context");
	}

	@Test
	void ofWhenHttps() {
		BaseUrl baseUrl = BaseUrl.of("https://localhost:8080/context");
		assertThat(baseUrl.isHttps()).isTrue();
		assertThat(baseUrl.resolve()).isEqualTo("https://localhost:8080/context");
	}

	@Test
	void ofWhenUppercaseHttps() {
		BaseUrl baseUrl = BaseUrl.of("HTTPS://localhost:8080/context");
		assertThat(baseUrl.isHttps()).isTrue();
		assertThat(baseUrl.resolve()).isEqualTo("HTTPS://localhost:8080/context");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void ofWhenUrlIssNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> BaseUrl.of(null)).withMessage("'url' must not be null");
	}

	@Test
	void of() {
		AtomicInteger atomicInteger = new AtomicInteger();
		BaseUrl baseUrl = BaseUrl.of(true, () -> String.valueOf(atomicInteger.incrementAndGet()));
		assertThat(atomicInteger.get()).isZero();
		assertThat(baseUrl.isHttps()).isTrue();
		assertThat(baseUrl.resolve()).isEqualTo("1");
		assertThat(baseUrl.resolve()).isEqualTo("2");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void ofWhenResolverIsNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> BaseUrl.of(true, null))
			.withMessage("'resolver' must not be null");
	}

	@Test
	void withPath() {
		BaseUrl baseUrl = BaseUrl.of("http://localhost");
		assertThat(baseUrl.withPath("/context").resolve("")).isEqualTo("http://localhost/context");
		assertThat(baseUrl.withPath("/context").withPath("/test").resolve("path"))
			.isEqualTo("http://localhost/context/test/path");
	}

	@Test
	void withPathInvokesParentResolver() {
		AtomicInteger atomicInteger = new AtomicInteger();
		BaseUrl baseUrl = BaseUrl.of(true,
				() -> "https://example.com/" + atomicInteger.incrementAndGet());
		assertThat(baseUrl.withPath("/context").resolve("")).isEqualTo("https://example.com/1/context");
		assertThat(baseUrl.withPath("/context").withPath("/test").resolve("path"))
				.isEqualTo("https://example.com/2/context/test/path");
	}

}
