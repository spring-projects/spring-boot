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

package org.springframework.boot.undertow.autoconfigure.servlet;

import io.undertow.Undertow.Builder;
import io.undertow.servlet.api.DeploymentInfo;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.undertow.UndertowBuilderCustomizer;
import org.springframework.boot.undertow.servlet.UndertowDeploymentInfoCustomizer;
import org.springframework.boot.undertow.servlet.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.server.autoconfigure.servlet.AbstractServletWebServerAutoConfigurationTests;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link UndertowServletWebServerAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Raheela Aslam
 * @author Madhura Bhave
 */
class UndertowServletWebServerAutoConfigurationTests extends AbstractServletWebServerAutoConfigurationTests {

	UndertowServletWebServerAutoConfigurationTests() {
		super(UndertowServletWebServerAutoConfiguration.class);
	}

	@Test
	void undertowDeploymentInfoCustomizerBeanIsAddedToFactory() {
		this.serverRunner.withUserConfiguration(UndertowDeploymentInfoCustomizerConfiguration.class).run((context) -> {
			UndertowServletWebServerFactory factory = context.getBean(UndertowServletWebServerFactory.class);
			UndertowDeploymentInfoCustomizer customizer = context.getBean("deploymentInfoCustomizer",
					UndertowDeploymentInfoCustomizer.class);
			assertThat(factory.getDeploymentInfoCustomizers()).contains(customizer);
		});
	}

	@Test
	void undertowDeploymentInfoCustomizerRegisteredAsBeanAndViaFactoryIsOnlyCalledOnce() {
		this.serverRunner.withUserConfiguration(DoubleRegistrationUndertowDeploymentInfoCustomizerConfiguration.class)
			.run((context) -> {
				UndertowServletWebServerFactory factory = context.getBean(UndertowServletWebServerFactory.class);
				UndertowDeploymentInfoCustomizer customizer = context.getBean("deploymentInfoCustomizer",
						UndertowDeploymentInfoCustomizer.class);
				assertThat(factory.getDeploymentInfoCustomizers()).contains(customizer);
				then(customizer).should().customize(any(DeploymentInfo.class));
			});
	}

	@Test
	void undertowBuilderCustomizerRegisteredAsBeanAndViaFactoryIsOnlyCalledOnce() {
		this.serverRunner.withConfiguration(AutoConfigurations.of(UndertowServletWebServerAutoConfiguration.class))
			.withUserConfiguration(DoubleRegistrationUndertowBuilderCustomizerConfiguration.class)
			.run((context) -> {
				UndertowServletWebServerFactory factory = context.getBean(UndertowServletWebServerFactory.class);
				UndertowBuilderCustomizer customizer = context.getBean("builderCustomizer",
						UndertowBuilderCustomizer.class);
				assertThat(factory.getBuilderCustomizers()).contains(customizer);
				then(customizer).should().customize(any(Builder.class));
			});
	}

	@Test
	void undertowBuilderCustomizerBeanIsAddedToFactory() {
		this.serverRunner.withUserConfiguration(UndertowBuilderCustomizerConfiguration.class).run((context) -> {
			UndertowServletWebServerFactory factory = context.getBean(UndertowServletWebServerFactory.class);
			assertThat(factory.getBuilderCustomizers())
				.contains(context.getBean("builderCustomizer", UndertowBuilderCustomizer.class));
		});
	}

	@Test
	void undertowServletWebServerFactoryCustomizerIsAutoConfigured() {
		this.serverRunner.withUserConfiguration(UndertowBuilderCustomizerConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(UndertowServletWebServerFactoryCustomizer.class));
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
		WebServerFactoryCustomizer<UndertowServletWebServerFactory> undertowCustomizer() {
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

	@Configuration(proxyBeanMethods = false)
	static class DoubleRegistrationUndertowDeploymentInfoCustomizerConfiguration {

		private final UndertowDeploymentInfoCustomizer customizer = mock(UndertowDeploymentInfoCustomizer.class);

		@Bean
		UndertowDeploymentInfoCustomizer deploymentInfoCustomizer() {
			return this.customizer;
		}

		@Bean
		WebServerFactoryCustomizer<UndertowServletWebServerFactory> undertowCustomizer() {
			return (undertow) -> undertow.addDeploymentInfoCustomizers(this.customizer);
		}

	}

}
