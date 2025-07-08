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

package org.springframework.boot.undertow.autoconfigure.reactive;

import io.undertow.Undertow.Builder;
import org.junit.jupiter.api.Test;

import org.springframework.boot.undertow.UndertowBuilderCustomizer;
import org.springframework.boot.undertow.reactive.UndertowReactiveWebServerFactory;
import org.springframework.boot.undertow.servlet.UndertowDeploymentInfoCustomizer;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.server.autoconfigure.reactive.AbstractReactiveWebServerAutoConfigurationTests;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link UndertowReactiveWebServerAutoConfiguration}.
 *
 * @author Brian Clozel
 * @author Raheela Aslam
 * @author Madhura Bhave
 * @author Scott Frederick
 */
class UndertowReactiveWebServerAutoConfigurationTests extends AbstractReactiveWebServerAutoConfigurationTests {

	UndertowReactiveWebServerAutoConfigurationTests() {
		super(UndertowReactiveWebServerAutoConfiguration.class);
	}

	@Test
	void undertowBuilderCustomizerBeanIsAddedToFactory() {
		this.serverRunner.withUserConfiguration(UndertowBuilderCustomizerConfiguration.class).run((context) -> {
			UndertowReactiveWebServerFactory factory = context.getBean(UndertowReactiveWebServerFactory.class);
			assertThat(factory.getBuilderCustomizers())
				.contains(context.getBean("builderCustomizer", UndertowBuilderCustomizer.class));
		});
	}

	@Test
	void undertowBuilderCustomizerRegisteredAsBeanAndViaFactoryIsOnlyCalledOnce() {
		this.serverRunner.withUserConfiguration(DoubleRegistrationUndertowBuilderCustomizerConfiguration.class)
			.run((context) -> {
				UndertowReactiveWebServerFactory factory = context.getBean(UndertowReactiveWebServerFactory.class);
				UndertowBuilderCustomizer customizer = context.getBean("builderCustomizer",
						UndertowBuilderCustomizer.class);
				assertThat(factory.getBuilderCustomizers()).contains(customizer);
				then(customizer).should().customize(any(Builder.class));
			});
	}

	@Configuration(proxyBeanMethods = false)
	static class UndertowBuilderCustomizerConfiguration {

		@Bean
		UndertowBuilderCustomizer builderCustomizer() {
			return (builder) -> {
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class DoubleRegistrationUndertowBuilderCustomizerConfiguration {

		private final UndertowBuilderCustomizer customizer = mock(UndertowBuilderCustomizer.class);

		@Bean
		UndertowBuilderCustomizer builderCustomizer() {
			return this.customizer;
		}

		@Bean
		WebServerFactoryCustomizer<UndertowReactiveWebServerFactory> undertowCustomizer() {
			return (undertow) -> undertow.addBuilderCustomizers(this.customizer);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class UndertowDeploymentInfoCustomizerConfiguration {

		@Bean
		UndertowDeploymentInfoCustomizer deploymentInfoCustomizer() {
			return (deploymentInfo) -> {
			};
		}

	}

}
