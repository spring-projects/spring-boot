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

package org.springframework.boot.test.autoconfigure.web.reactive;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.codec.CodecConfigurer;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link WebTestClientAutoConfiguration}
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 */
class WebTestClientAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(WebTestClientAutoConfiguration.class));

	@Test
	void shouldNotBeConfiguredWithoutWebHandler() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasNotFailed();
			assertThat(context).doesNotHaveBean(WebTestClient.class);
		});
	}

	@Test
	void shouldCustomizeClientCodecs() {
		this.contextRunner.withUserConfiguration(CodecConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(WebTestClient.class);
			assertThat(context).hasSingleBean(CodecCustomizer.class);
			verify(context.getBean(CodecCustomizer.class)).customize(any(CodecConfigurer.class));
		});
	}

	@Test
	void shouldCustomizeTimeout() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
				.withPropertyValues("spring.test.webtestclient.timeout=15m").run((context) -> {
					WebTestClient webTestClient = context.getBean(WebTestClient.class);
					assertThat(webTestClient).hasFieldOrPropertyWithValue("responseTimeout", Duration.ofMinutes(15));
				});
	}

	@Test
	@SuppressWarnings("unchecked")
	void shouldApplySpringSecurityConfigurer() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class).run((context) -> {
			WebTestClient webTestClient = context.getBean(WebTestClient.class);
			WebTestClient.Builder builder = (WebTestClient.Builder) ReflectionTestUtils.getField(webTestClient,
					"builder");
			WebHttpHandlerBuilder httpHandlerBuilder = (WebHttpHandlerBuilder) ReflectionTestUtils.getField(builder,
					"httpHandlerBuilder");
			List<WebFilter> filters = (List<WebFilter>) ReflectionTestUtils.getField(httpHandlerBuilder, "filters");
			assertThat(filters.get(0).getClass().getName()).isEqualTo(
					"org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers$MutatorFilter");
		});
	}

	@Test
	@SuppressWarnings("unchecked")
	void shouldNotApplySpringSecurityConfigurerWhenSpringSecurityNotOnClassPath() {
		FilteredClassLoader classLoader = new FilteredClassLoader(SecurityMockServerConfigurers.class);
		this.contextRunner.withUserConfiguration(BaseConfiguration.class).withClassLoader(classLoader)
				.run((context) -> {
					WebTestClient webTestClient = context.getBean(WebTestClient.class);
					WebTestClient.Builder builder = (WebTestClient.Builder) ReflectionTestUtils.getField(webTestClient,
							"builder");
					WebHttpHandlerBuilder httpHandlerBuilder = (WebHttpHandlerBuilder) ReflectionTestUtils
							.getField(builder, "httpHandlerBuilder");
					List<WebFilter> filters = (List<WebFilter>) ReflectionTestUtils.getField(httpHandlerBuilder,
							"filters");
					assertThat(filters).isEmpty();
				});
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

}
