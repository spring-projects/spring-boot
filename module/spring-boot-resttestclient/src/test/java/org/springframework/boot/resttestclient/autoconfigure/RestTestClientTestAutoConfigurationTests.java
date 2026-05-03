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

import org.assertj.core.extractor.Extractors;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.http.converter.autoconfigure.ClientHttpMessageConvertersCustomizer;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.test.http.server.LocalTestWebServer;
import org.springframework.boot.test.http.server.LocalTestWebServer.Scheme;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.HttpMessageConverters.ClientBuilder;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilderFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RestTestClientTestAutoConfiguration}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class RestTestClientTestAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(RestTestClientTestAutoConfiguration.class));

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
					org.springframework.boot.test.http.server.LocalTestWebServer$Provider=\
					org.springframework.boot.resttestclient.autoconfigure.RestTestClientTestAutoConfigurationTests$TestLocalTestWebServerProvider
					""")
	void shouldDefineRestTestClientBoundToWebServer() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(RestTestClient.class).hasBean("restTestClient");
			RestTestClient client = context.getBean(RestTestClient.class);
			UriBuilderFactory uiBuilderFactory = (UriBuilderFactory) Extractors
				.byName("restTestClientBuilder.restClientBuilder.uriBuilderFactory")
				.apply(client);
			assertThat(uiBuilderFactory.uriString("").toUriString()).isEqualTo("https://localhost:8182");
		});
	}

	@Test
	void clientHttpMessageConverterCustomizersAreAppliedInOrder() {
		this.contextRunner.withUserConfiguration(ClientHttpMessageConverterCustomizersConfiguration.class)
			.run((context) -> {
				ClientHttpMessageConvertersCustomizer customizer1 = context.getBean("customizer1",
						ClientHttpMessageConvertersCustomizer.class);
				ClientHttpMessageConvertersCustomizer customizer2 = context.getBean("customizer2",
						ClientHttpMessageConvertersCustomizer.class);
				ClientHttpMessageConvertersCustomizer customizer3 = context.getBean("customizer3",
						ClientHttpMessageConvertersCustomizer.class);
				InOrder inOrder = inOrder(customizer1, customizer2, customizer3);
				inOrder.verify(customizer3).customize(any(ClientBuilder.class));
				inOrder.verify(customizer1).customize(any(ClientBuilder.class));
				inOrder.verify(customizer2).customize(any(ClientBuilder.class));
			});
	}

	@Configuration(proxyBeanMethods = false)
	static class RestTestClientCustomConfig {

		@Bean
		RestTestClientBuilderCustomizer myRestTestClientCustomizer() {
			return mock(RestTestClientBuilderCustomizer.class);
		}

	}

	@SuppressWarnings("unused")
	static class TestLocalTestWebServerProvider implements LocalTestWebServer.Provider {

		@Override
		public @Nullable LocalTestWebServer getLocalTestWebServer() {
			return LocalTestWebServer.of(Scheme.HTTPS, 8182);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ClientHttpMessageConverterCustomizersConfiguration {

		@Bean
		@Order(-5)
		ClientHttpMessageConvertersCustomizer customizer1() {
			return mock(ClientHttpMessageConvertersCustomizer.class);
		}

		@Bean
		@Order(5)
		ClientHttpMessageConvertersCustomizer customizer2() {
			return mock(ClientHttpMessageConvertersCustomizer.class);
		}

		@Bean
		@Order(-10)
		ClientHttpMessageConvertersCustomizer customizer3() {
			return mock(ClientHttpMessageConvertersCustomizer.class);
		}

	}

}
