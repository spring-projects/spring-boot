/*
 * Copyright 2012-2019 the original author or authors.
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

import org.springframework.http.client.reactive.JettyClientHttpConnector;
import org.springframework.http.client.reactive.JettyResourceFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ClientHttpConnectorConfiguration}.
 *
 * @author Phillip Webb
 */
class ClientHttpConnectorConfigurationTests {

	@Test
	void jettyClientHttpConnectorAppliesJettyResourceFactory() {
		Executor executor = mock(Executor.class);
		ByteBufferPool byteBufferPool = mock(ByteBufferPool.class);
		Scheduler scheduler = mock(Scheduler.class);
		JettyResourceFactory jettyResourceFactory = new JettyResourceFactory();
		jettyResourceFactory.setExecutor(executor);
		jettyResourceFactory.setByteBufferPool(byteBufferPool);
		jettyResourceFactory.setScheduler(scheduler);
		JettyClientHttpConnector connector = getClientHttpConnector(jettyResourceFactory);
		HttpClient httpClient = (HttpClient) ReflectionTestUtils.getField(connector, "httpClient");
		assertThat(httpClient.getExecutor()).isSameAs(executor);
		assertThat(httpClient.getByteBufferPool()).isSameAs(byteBufferPool);
		assertThat(httpClient.getScheduler()).isSameAs(scheduler);
	}

	@Test
	void JettyResourceFactoryHasSslContextFactory() {
		// gh-16810
		JettyResourceFactory jettyResourceFactory = new JettyResourceFactory();
		JettyClientHttpConnector connector = getClientHttpConnector(jettyResourceFactory);
		HttpClient httpClient = (HttpClient) ReflectionTestUtils.getField(connector, "httpClient");
		assertThat(httpClient.getSslContextFactory()).isNotNull();
	}

	private JettyClientHttpConnector getClientHttpConnector(JettyResourceFactory jettyResourceFactory) {
		ClientHttpConnectorConfiguration.JettyClient jettyClient = new ClientHttpConnectorConfiguration.JettyClient();
		// We shouldn't usually call this method directly since it's on a non-proxy config
		return ReflectionTestUtils.invokeMethod(jettyClient, "jettyClientHttpConnector", jettyResourceFactory);
	}

}
