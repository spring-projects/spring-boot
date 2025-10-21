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

import java.net.URI;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.http.server.LocalTestWebServer.BaseUriDetails;
import org.springframework.boot.test.http.server.LocalTestWebServer.Scheme;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriBuilderFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link LocalTestWebServer}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class LocalTestWebServerTests {

	private final LocalTestWebServer server = LocalTestWebServer.of(Scheme.HTTPS, 8080, "");

	@Test
	void schemeWhenHttpsSchemeReturnsHttpsScheme() {
		assertThat(LocalTestWebServer.of(Scheme.HTTPS, 8080, "").scheme()).isEqualTo(Scheme.HTTPS);
	}

	@Test
	void schemeWhenHttpSchemeReturnsHttpScheme() {
		assertThat(LocalTestWebServer.of(Scheme.HTTP, 8080, "").scheme()).isEqualTo(Scheme.HTTP);
	}

	@Test
	void uriBuilderWhenHasSlashUriUsesLocalServer() {
		UriBuilder builder = this.server.uriBuilder("/");
		assertThat(builder.toUriString()).isEqualTo("https://localhost:8080/");
	}

	@Test
	void uriBuilderWhenHasEmptyUriUsesLocalServer() {
		UriBuilder builder = this.server.uriBuilder("");
		assertThat(builder.toUriString()).isEqualTo("https://localhost:8080");
	}

	@Test
	void uriBuilderWhenHasNestedPathUsesLocalServer() {
		UriBuilder builder = this.server.uriBuilder("/foo/bar");
		assertThat(builder.toUriString()).isEqualTo("https://localhost:8080/foo/bar");
	}

	@Test
	void uriBuilderWhenHasPathNoStartingWithSlashUsesLocalServer() {
		UriBuilder builder = this.server.uriBuilder("foo/bar");
		assertThat(builder.toUriString()).isEqualTo("https://localhost:8080/foo/bar");
	}

	@Test
	void uriBuilderWhenHasFullUriDoesNotUseLocalServer() {
		UriBuilder builder = this.server.uriBuilder("https://sub.example.com");
		assertThat(builder.toUriString()).isEqualTo("https://sub.example.com");
	}

	@Test
	void uriBuilderFactoryExpandWithMap() {
		UriBuilderFactory factory = this.server.uriBuilderFactory();
		assertThat(factory.expand("/test/{name}", Map.of("name", "value")))
			.isEqualTo(URI.create("https://localhost:8080/test/value"));
	}

	@Test
	void uriBuilderFactoryExpandsWithMap() {
		UriBuilderFactory factory = this.server.uriBuilderFactory();
		assertThat(factory.expand("/test/{name}", "value")).isEqualTo(URI.create("https://localhost:8080/test/value"));
	}

	@Test
	void uriBuilderFactoryExpandsWithVariables() {
		UriBuilderFactory factory = this.server.uriBuilderFactory();
		assertThat(factory.uriString("https://example.com").build()).isEqualTo(URI.create("https://example.com"));
	}

	@Test
	void uriWhenHttp() {
		assertThat(LocalTestWebServer.of(Scheme.HTTP, 8080, "").uri()).isEqualTo("http://localhost:8080");
	}

	@Test
	void uriWhenHttps() {
		assertThat(LocalTestWebServer.of(Scheme.HTTPS, 4343, "").uri()).isEqualTo("https://localhost:4343");
	}

	@Test
	void uriWhenHasPath() {
		assertThat(LocalTestWebServer.of(Scheme.HTTPS, 8080, "/path").uri()).isEqualTo("https://localhost:8080/path");
	}

	@Test
	void uriWithUri() {
		assertThat(this.server.uri(null)).isEqualTo("https://localhost:8080");
		assertThat(this.server.uri("")).isEqualTo("https://localhost:8080");
		assertThat(this.server.uri("/")).isEqualTo("https://localhost:8080/");
		assertThat(this.server.uri("/foo")).isEqualTo("https://localhost:8080/foo");
		assertThat(this.server.uri("https://example.com/foo")).isEqualTo("https://example.com/foo");
	}

	@Test
	void uriUsesSingletonBaseUriDetails() {
		AtomicInteger counter = new AtomicInteger();
		LocalTestWebServer server = LocalTestWebServer.of(Scheme.HTTPS,
				() -> new BaseUriDetails(8080, "/" + counter.incrementAndGet()));
		assertThat(server.uri()).isEqualTo("https://localhost:8080/1");
		assertThat(server.uri()).isEqualTo("https://localhost:8080/1");
	}

	@Test
	void uriBuilderFactoryUsesSingletonUriBuilderFactory() {
		LocalTestWebServer server = LocalTestWebServer.of(Scheme.HTTPS, () -> new BaseUriDetails(8080, "/"));
		UriBuilderFactory uriBuilderFactory = server.uriBuilderFactory();
		assertThat(server.uriBuilderFactory()).isSameAs(uriBuilderFactory);
	}

	@Test
	void withPathCreatedNewInstance() {
		assertThat(LocalTestWebServer.of(Scheme.HTTPS, 8080, "/path").withPath("/other").uri())
			.isEqualTo("https://localhost:8080/path/other");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void ofWhenBaseUriDetailsSupplierIsNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> LocalTestWebServer.of(Scheme.HTTPS, null))
			.withMessage("'baseUriDetailsSupplier' must not be null");
	}

	@Test
	@WithResource(name = "META-INF/spring.factories", content = """
			org.springframework.boot.test.http.server.LocalTestWebServer$Provider=\
			org.springframework.boot.test.http.server.LocalTestWebServerTests$Provider1,\
			org.springframework.boot.test.http.server.LocalTestWebServerTests$Provider2,\
			org.springframework.boot.test.http.server.LocalTestWebServerTests$Provider3
			""")
	void getReturnsFirstProvided() {
		ApplicationContext applicationContext = new GenericApplicationContext();
		LocalTestWebServer provided = LocalTestWebServer.get(applicationContext);
		assertThat(provided).isNotNull();
		assertThat(provided.uri()).isEqualTo("https://localhost:7070/p2");
	}

	@Test
	@WithResource(name = "META-INF/spring.factories", content = """
			org.springframework.boot.test.http.server.LocalTestWebServer$Provider=\
			org.springframework.boot.test.http.server.LocalTestWebServerTests$Provider1
			""")
	void getWhenNoneReturnsNull() {
		ApplicationContext applicationContext = new GenericApplicationContext();
		LocalTestWebServer provided = LocalTestWebServer.get(applicationContext);
		assertThat(provided).isNull();
	}

	@Test
	@WithResource(name = "META-INF/spring.factories", content = """
			org.springframework.boot.test.http.server.LocalTestWebServer$Provider=\
			org.springframework.boot.test.http.server.LocalTestWebServerTests$Provider1
			""")
	void obtainWhenNoneProvidedThrowsException() {
		ApplicationContext applicationContext = new GenericApplicationContext();
		assertThatIllegalStateException().isThrownBy(() -> LocalTestWebServer.obtain(applicationContext))
			.withMessage("No local test web server available");
	}

	@SuppressWarnings("unused")
	static class Provider1 implements LocalTestWebServer.Provider {

		@Override
		public @Nullable LocalTestWebServer getLocalTestWebServer() {
			return null;
		}

	}

	@SuppressWarnings("unused")
	static class Provider2 implements LocalTestWebServer.Provider {

		@Override
		public @Nullable LocalTestWebServer getLocalTestWebServer() {
			return LocalTestWebServer.of(Scheme.HTTPS, 7070, "/p2");
		}

	}

	@SuppressWarnings("unused")
	static class Provider3 implements LocalTestWebServer.Provider {

		@Override
		public @Nullable LocalTestWebServer getLocalTestWebServer() {
			throw new IllegalStateException();
		}

	}

}
