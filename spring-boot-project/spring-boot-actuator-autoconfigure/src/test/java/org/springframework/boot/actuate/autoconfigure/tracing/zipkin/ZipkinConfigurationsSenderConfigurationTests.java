/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.tracing.zipkin;

import java.net.http.HttpClient;

import org.junit.jupiter.api.Test;
import zipkin2.reporter.BytesMessageSender;
import zipkin2.reporter.HttpEndpointSupplier;
import zipkin2.reporter.urlconnection.URLConnectionSender;

import org.springframework.boot.actuate.autoconfigure.tracing.zipkin.ZipkinConfigurations.SenderConfiguration;
import org.springframework.boot.actuate.autoconfigure.tracing.zipkin.ZipkinConfigurations.UrlConnectionSenderConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SenderConfiguration}.
 *
 * @author Moritz Halbritter
 * @author Wick Dynex
 */
@SuppressWarnings({ "deprecation", "removal" })
class ZipkinConfigurationsSenderConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(DefaultEncodingConfiguration.class, SenderConfiguration.class));

	@Test
	void shouldSupplyDefaultHttpClientSenderBean() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(BytesMessageSender.class);
			assertThat(context).hasSingleBean(ZipkinHttpClientSender.class);
			assertThat(context).doesNotHaveBean(URLConnectionSender.class);
		});
	}

	@Test
	void shouldUseUrlConnectionSenderIfHttpClientIsNotAvailable() {
		this.contextRunner.withUserConfiguration(UrlConnectionSenderConfiguration.class)
			.withClassLoader(new FilteredClassLoader(HttpClient.class))
			.run((context) -> {
				assertThat(context).doesNotHaveBean(ZipkinHttpClientSender.class);
				assertThat(context).hasSingleBean(BytesMessageSender.class);
				assertThat(context).hasSingleBean(URLConnectionSender.class);
			});
	}

	@Test
	void shouldBackOffOnCustomBeans() {
		this.contextRunner.withUserConfiguration(CustomConfiguration.class).run((context) -> {
			assertThat(context).hasBean("customSender");
			assertThat(context).hasSingleBean(BytesMessageSender.class);
		});
	}

	@Test
	void shouldUseCustomHttpEndpointSupplierFactory() {
		this.contextRunner.withUserConfiguration(CustomHttpEndpointSupplierFactoryConfiguration.class)
			.withClassLoader(new FilteredClassLoader(HttpClient.class))
			.run((context) -> {
				URLConnectionSender urlConnectionSender = context.getBean(URLConnectionSender.class);
				assertThat(urlConnectionSender).extracting("delegate.endpointSupplier")
					.isInstanceOf(CustomHttpEndpointSupplier.class);
			});
	}

	@Configuration(proxyBeanMethods = false)
	private static final class CustomConfiguration {

		@Bean
		BytesMessageSender customSender() {
			return mock(BytesMessageSender.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	private static final class CustomHttpEndpointSupplierFactoryConfiguration {

		@Bean
		HttpEndpointSupplier.Factory httpEndpointSupplier() {
			return new CustomHttpEndpointSupplierFactory();
		}

	}

	private static final class CustomHttpEndpointSupplierFactory implements HttpEndpointSupplier.Factory {

		@Override
		public HttpEndpointSupplier create(String endpoint) {
			return new CustomHttpEndpointSupplier(endpoint);
		}

	}

	private record CustomHttpEndpointSupplier(String endpoint) implements HttpEndpointSupplier {

		@Override
		public String get() {
			return this.endpoint;
		}

		@Override
		public void close() {
		}
	}

}
