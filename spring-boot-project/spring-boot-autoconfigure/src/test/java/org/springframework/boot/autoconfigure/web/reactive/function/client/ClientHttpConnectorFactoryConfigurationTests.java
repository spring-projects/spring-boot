/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.reactive.function.client;

import java.util.concurrent.Executor;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.util.thread.Scheduler;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundleKey;
import org.springframework.boot.ssl.jks.JksSslStoreBundle;
import org.springframework.boot.ssl.jks.JksSslStoreDetails;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.reactive.HttpComponentsClientHttpConnector;
import org.springframework.http.client.reactive.JettyClientHttpConnector;
import org.springframework.http.client.reactive.JettyResourceFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Tests for {@link ClientHttpConnectorFactoryConfiguration}.
 *
 * @author Phillip Webb
 * @author Brian Clozel
 * @author Moritz Halbritter
 */
class ClientHttpConnectorFactoryConfigurationTests {

	@Test
	void jettyClientHttpConnectorAppliesJettyResourceFactory() {
		Executor executor = mock(Executor.class);
		ByteBufferPool byteBufferPool = mock(ByteBufferPool.class);
		Scheduler scheduler = mock(Scheduler.class);
		JettyResourceFactory jettyResourceFactory = new JettyResourceFactory();
		jettyResourceFactory.setExecutor(executor);
		jettyResourceFactory.setByteBufferPool(byteBufferPool);
		jettyResourceFactory.setScheduler(scheduler);
		JettyClientHttpConnectorFactory connectorFactory = getJettyClientHttpConnectorFactory(jettyResourceFactory);
		JettyClientHttpConnector connector = connectorFactory.createClientHttpConnector();
		HttpClient httpClient = (HttpClient) ReflectionTestUtils.getField(connector, "httpClient");
		assertThat(httpClient.getExecutor()).isSameAs(executor);
		assertThat(httpClient.getByteBufferPool()).isSameAs(byteBufferPool);
		assertThat(httpClient.getScheduler()).isSameAs(scheduler);
	}

	@Test
	void JettyResourceFactoryHasSslContextFactory() {
		// gh-16810
		JettyResourceFactory jettyResourceFactory = new JettyResourceFactory();
		JettyClientHttpConnectorFactory connectorFactory = getJettyClientHttpConnectorFactory(jettyResourceFactory);
		JettyClientHttpConnector connector = connectorFactory.createClientHttpConnector();
		HttpClient httpClient = (HttpClient) ReflectionTestUtils.getField(connector, "httpClient");
		assertThat(httpClient.getSslContextFactory()).isNotNull();
	}

	private JettyClientHttpConnectorFactory getJettyClientHttpConnectorFactory(
			JettyResourceFactory jettyResourceFactory) {
		ClientHttpConnectorFactoryConfiguration.JettyClient jettyClient = new ClientHttpConnectorFactoryConfiguration.JettyClient();
		// We shouldn't usually call this method directly since it's on a non-proxy config
		return ReflectionTestUtils.invokeMethod(jettyClient, "jettyClientHttpConnectorFactory", jettyResourceFactory);
	}

	@Test
	void shouldApplyHttpClientMapper() {
		JksSslStoreDetails storeDetails = JksSslStoreDetails.forLocation("classpath:test.jks");
		JksSslStoreBundle stores = new JksSslStoreBundle(storeDetails, storeDetails);
		SslBundle sslBundle = spy(SslBundle.of(stores, SslBundleKey.of("password")));
		new ReactiveWebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(ClientHttpConnectorFactoryConfiguration.ReactorNetty.class))
			.withUserConfiguration(CustomHttpClientMapper.class)
			.run((context) -> {
				context.getBean(ReactorClientHttpConnectorFactory.class).createClientHttpConnector(sslBundle);
				assertThat(CustomHttpClientMapper.called).isTrue();
				then(sslBundle).should().getManagers();
			});
	}

	@Test
	void shouldNotConfigureReactiveHttpClient5WhenHttpCore5ReactiveJarIsMissing() {
		new ReactiveWebApplicationContextRunner()
			.withClassLoader(new FilteredClassLoader("org.apache.hc.core5.reactive"))
			.withConfiguration(AutoConfigurations.of(ClientHttpConnectorFactoryConfiguration.HttpClient5.class))
			.run((context) -> assertThat(context).doesNotHaveBean(HttpComponentsClientHttpConnector.class));
	}

	static class CustomHttpClientMapper {

		static boolean called = false;

		@Bean
		ReactorNettyHttpClientMapper clientMapper() {
			return (client) -> {
				called = true;
				return client.baseUrl("/test");
			};
		}

	}

}
