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

package org.springframework.boot.resttestclient.autoconfigure;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.test.http.client.BaseUrlUriBuilderFactory;
import org.springframework.boot.test.http.server.BaseUrl;
import org.springframework.boot.test.http.server.BaseUrlProvider;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RestTestClientAutoConfiguration}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class RestTestClientAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(RestTestClientAutoConfiguration.class));

	@Test
	void registersRestTestClient() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(RestTestClient.class));
	}

	@Test
	void shouldNotRegisterRestTestClientIfRestClientIsMissing() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(RestClient.class))
			.run((context) -> assertThat(context).doesNotHaveBean(RestTestClient.class));
	}

	@Test
	void shouldApplyRestTestClientCustomizers() {
		this.contextRunner.withUserConfiguration(RestTestClientCustomConfig.class).run((context) -> {
			assertThat(context).hasSingleBean(RestTestClient.class);
			assertThat(context).hasBean("myRestTestClientCustomizer");
			then(context.getBean("myRestTestClientCustomizer", RestTestClientBuilderCustomizer.class)).should()
				.customize(any(RestTestClient.Builder.class));
		});
	}

	@Test
	@WithResource(name = "META-INF/spring.factories",
			content = """
					org.springframework.boot.test.http.server.BaseUrlProvider=\
					org.springframework.boot.resttestclient.autoconfigure.RestTestClientAutoConfigurationTests.TestBaseUrlProvider
					""")
	void shouldDefineWebTestClientBoundToWebServer() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(RestTestClient.class);
			assertThat(context).hasBean("restTestClient");
			RestTestClient client = context.getBean(RestTestClient.class);
			assertThat(client).extracting("restTestClientBuilder")
				.extracting("restClientBuilder")
				.extracting("uriBuilderFactory")
				.isInstanceOf(BaseUrlUriBuilderFactory.class);
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class RestTestClientCustomConfig {

		@Bean
		RestTestClientBuilderCustomizer myRestTestClientCustomizer() {
			return mock(RestTestClientBuilderCustomizer.class);
		}

	}

	static class TestBaseUrlProvider implements BaseUrlProvider {

		@Override
		public @Nullable BaseUrl getBaseUrl() {
			return BaseUrl.of("https://localhost:8080");
		}

	}

}
