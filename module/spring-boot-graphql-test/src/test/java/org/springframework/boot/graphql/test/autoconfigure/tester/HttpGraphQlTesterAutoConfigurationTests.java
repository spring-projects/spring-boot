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

package org.springframework.boot.graphql.test.autoconfigure.tester;

import java.net.URI;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.http.server.LocalTestWebServer;
import org.springframework.boot.test.http.server.LocalTestWebServer.Scheme;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.boot.webtestclient.WebTestClientAutoConfiguration;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriBuilderFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HttpGraphQlTesterAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
class HttpGraphQlTesterAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(HttpGraphQlTesterAutoConfiguration.class));

	@Test
	void shouldNotContributeTesterIfWebTestClientNotPresent() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(HttpGraphQlTester.class));
	}

	@Test
	void shouldContributeTesterBoutToMockMvc() {
		this.contextRunner.withBean(MockMvc.class, () -> mock(MockMvc.class))
			.withConfiguration(AutoConfigurations.of(WebTestClientAutoConfiguration.class))
			.run((context) -> {
				assertThat(context).hasSingleBean(HttpGraphQlTester.class);
				assertThat(context.getBean(HttpGraphQlTester.class)).extracting("webTestClient")
					.extracting("builder")
					.extracting("baseUrl")
					.isEqualTo("/graphql");
			});
	}

	@Test
	@WithResource(name = "META-INF/spring.factories",
			content = """
					org.springframework.boot.test.http.server.LocalTestWebServer$Provider=\
					org.springframework.boot.graphql.test.autoconfigure.tester.HttpGraphQlTesterAutoConfigurationTests$TestLocalTestWebServerProvider
					""")
	void shouldContributeTesterBoundToHttpServer() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(WebTestClientAutoConfiguration.class))
			.run((context) -> {
				assertThat(context).hasSingleBean(HttpGraphQlTester.class);
				assertThat(context.getBean(HttpGraphQlTester.class)).extracting("webTestClient")
					.extracting("builder")
					.extracting("uriBuilderFactory")
					.asInstanceOf(InstanceOfAssertFactories.type(UriBuilderFactory.class))
					.satisfies((uriBuilderFactory) -> assertThat(uriBuilderFactory.uriString("/something").build())
						.isEqualTo(URI.create("https://localhost:4242/graphql/something")));
			});
	}

	@Test
	@WithResource(name = "META-INF/spring.factories",
			content = """
					org.springframework.boot.test.http.server.LocalTestWebServer$Provider=\
					org.springframework.boot.graphql.test.autoconfigure.tester.HttpGraphQlTesterAutoConfigurationTests$TestLocalTestWebServerProvider
					""")
	void shouldContributeTesterBoundToHttpServerUsingCustomGraphQlHttpPath() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(WebTestClientAutoConfiguration.class))
			.withPropertyValues("spring.graphql.http.path=/api/graphql")
			.run((context) -> {
				assertThat(context).hasSingleBean(HttpGraphQlTester.class);
				assertThat(context.getBean(HttpGraphQlTester.class)).extracting("webTestClient")
					.extracting("builder")
					.extracting("uriBuilderFactory")
					.asInstanceOf(InstanceOfAssertFactories.type(UriBuilderFactory.class))
					.satisfies((uriBuilderFactory) -> assertThat(uriBuilderFactory.uriString("/something").build())
						.isEqualTo(URI.create("https://localhost:4242/api/graphql/something")));
			});
	}

	@SuppressWarnings("unused")
	static class TestLocalTestWebServerProvider implements LocalTestWebServer.Provider {

		@Override
		public @Nullable LocalTestWebServer getLocalTestWebServer() {
			return LocalTestWebServer.of(Scheme.HTTPS, 4242);
		}

	}

}
