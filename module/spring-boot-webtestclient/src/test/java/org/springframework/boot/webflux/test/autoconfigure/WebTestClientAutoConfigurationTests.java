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

package org.springframework.boot.webflux.test.autoconfigure;

import java.time.Duration;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.http.codec.CodecCustomizer;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.http.server.LocalTestWebServer;
import org.springframework.boot.test.http.server.LocalTestWebServer.Scheme;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.boot.webtestclient.WebTestClientAutoConfiguration;
import org.springframework.boot.webtestclient.WebTestClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.http.codec.CodecConfigurer;
import org.springframework.test.web.reactive.server.HttpHandlerConnector;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.MockMvcHttpConnector;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.WebHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link WebTestClientAutoConfiguration}
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class WebTestClientAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(WebTestClientAutoConfiguration.class));

	@Test
	void shouldDefineWebTestClientBoundToHttpHandler() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(WebTestClient.class);
			assertThat(context).hasBean("webTestClient");
			assertThat(context.getBean(WebTestClient.class)).extracting("wiretapConnector")
				.extracting("delegate")
				.isInstanceOf(HttpHandlerConnector.class);
		});
	}

	@Test
	void shouldFailWhenDefaultWebHandlerIsNotAvailable() {
		this.contextRunner.withBean("myWebHandler", WebHandler.class, () -> mock(WebHandler.class)).run((context) -> {
			assertThat(context).hasFailed();
			assertThat(context).getFailure()
				.rootCause()
				.isInstanceOf(RuntimeException.class)
				.hasMessageStartingWith("Mock WebTestClient support requires")
				.hasMessageContaining("WebHandler");
		});
	}

	@Test
	void shouldFailWithNeitherDefaultWebHandlerNorWebApplicationContext() {
		ClassLoader parentClassLoader = Thread.currentThread().getContextClassLoader();
		this.contextRunner.withClassLoader(new FilteredClassLoader(parentClassLoader, WebApplicationContext.class))
			.run((context) -> {
				assertThat(context).hasFailed();
				assertThat(context).getFailure()
					.rootCause()
					.isInstanceOf(RuntimeException.class)
					.hasMessageStartingWith("Mock WebTestClient support requires")
					.hasMessageContaining("WebApplicationContext");
			});
	}

	@Test
	@WithResource(name = "META-INF/spring.factories",
			content = """
					org.springframework.boot.test.http.server.LocalTestWebServer$Provider=\
					org.springframework.boot.webflux.test.autoconfigure.WebTestClientAutoConfigurationTests$TestLocalTestWebServerProvider
					""")
	void shouldDefineWebTestClientBoundToWebServer() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(WebTestClient.class);
			assertThat(context).hasBean("webTestClient");
			assertThat(context.getBean(WebTestClient.class)).extracting("wiretapConnector")
				.extracting("delegate")
				.isInstanceOf(JdkClientHttpConnector.class);
		});
	}

	@Test
	void failsWithMockBaseUrlAndNoWebHandlerOrMockMvcBean() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasFailed();
			assertThat(context).getFailure()
				.rootCause()
				.isInstanceOf(RuntimeException.class)
				.hasMessageStartingWith("Mock WebTestClient support requires");
		});
	}

	@Test
	void shouldCustomizeClientCodecs() {
		this.contextRunner.withUserConfiguration(CodecConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(WebTestClient.class);
			assertThat(context).hasSingleBean(CodecCustomizer.class);
			then(context.getBean(CodecCustomizer.class)).should().customize(any(CodecConfigurer.class));
		});
	}

	@Test
	void shouldHaveConsistentDefaultTimeout() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class).run((context) -> {
			WebTestClient webTestClient = context.getBean(WebTestClient.class);
			assertThat(webTestClient).hasFieldOrPropertyWithValue("responseTimeout", Duration.ofSeconds(5));
		});
	}

	@Test
	void shouldCustomizeTimeout() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
			.withPropertyValues("spring.test.webtestclient.timeout=15m")
			.run((context) -> {
				WebTestClient webTestClient = context.getBean(WebTestClient.class);
				assertThat(webTestClient).hasFieldOrPropertyWithValue("responseTimeout", Duration.ofMinutes(15));
			});
	}

	@Test
	void shouldBackOffWithoutCodecCustomizer() {
		FilteredClassLoader classLoader = new FilteredClassLoader(CodecCustomizer.class);
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
			.withClassLoader(classLoader)
			.run((context) -> assertThat(context).doesNotHaveBean(WebTestClient.class));
	}

	@Test
	void shouldCreateMockMvcBasedWebTestClientWhenMockMvcBeanIsPresent() {
		this.contextRunner.withBean(MockMvc.class, () -> mock(MockMvc.class))
			.withUserConfiguration(WebTestClientCustomConfig.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(WebTestClient.class);
				assertThat(context).hasBean("webTestClient");
				assertThat(context).hasBean("myWebTestClientCustomizer");
				then(context.getBean("myWebTestClientCustomizer", WebTestClientBuilderCustomizer.class)).should()
					.customize(any(WebTestClient.Builder.class));
				WebTestClient webTestClient = context.getBean(WebTestClient.class);
				assertThat(webTestClient).extracting("builder")
					.extracting("connector")
					.isInstanceOf(MockMvcHttpConnector.class);
			});
	}

	@Test
	@WithResource(name = "META-INF/spring.factories",
			content = """
					org.springframework.boot.test.http.server.LocalTestWebServer$Provider=\
					org.springframework.boot.webflux.test.autoconfigure.WebTestClientAutoConfigurationTests$TestLocalTestWebServerProvider
					""")
	void shouldWorkWithoutServletStack() {
		ClassLoader parentClassLoader = Thread.currentThread().getContextClassLoader();
		this.contextRunner.withClassLoader(new FilteredClassLoader(parentClassLoader, WebApplicationContext.class))
			.run((context) -> {
				assertThat(context).hasSingleBean(WebTestClient.class);
				assertThat(context).hasBean("webTestClient");
				assertThat(context.getBean(WebTestClient.class)).extracting("wiretapConnector")
					.extracting("delegate")
					.isInstanceOf(JdkClientHttpConnector.class);
			});
	}

	@Configuration(proxyBeanMethods = false)
	static class WebTestClientCustomConfig {

		@Bean
		WebTestClientBuilderCustomizer myWebTestClientCustomizer() {
			return mock(WebTestClientBuilderCustomizer.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class BaseConfiguration {

		@Bean
		WebHandler webHandler() {
			return mock(WebHandler.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class CodecConfiguration {

		@Bean
		CodecCustomizer myCodecCustomizer() {
			return mock(CodecCustomizer.class);
		}

	}

	@SuppressWarnings("unused")
	static class TestLocalTestWebServerProvider implements LocalTestWebServer.Provider {

		@Override
		public @Nullable LocalTestWebServer getLocalTestWebServer() {
			return LocalTestWebServer.of(Scheme.HTTPS, 8080);
		}

	}

}
