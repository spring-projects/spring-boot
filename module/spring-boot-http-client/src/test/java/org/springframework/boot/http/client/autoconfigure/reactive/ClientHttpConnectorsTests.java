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

package org.springframework.boot.http.client.autoconfigure.reactive;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.http.client.HttpRedirects;
import org.springframework.boot.http.client.autoconfigure.reactive.AbstractClientHttpConnectorProperties.Connector;
import org.springframework.boot.http.client.reactive.ClientHttpConnectorSettings;
import org.springframework.boot.http.client.reactive.JettyClientHttpConnectorBuilder;
import org.springframework.boot.http.client.reactive.ReactorClientHttpConnectorBuilder;
import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ClientHttpConnectors}.
 *
 * @author Phillip Webb
 */
class ClientHttpConnectorsTests {

	private final DefaultSslBundleRegistry bundleRegistry = new DefaultSslBundleRegistry();

	private ObjectFactory<SslBundles> sslBundles = () -> this.bundleRegistry;

	@Test
	void builderWhenHasConnectorPropertyReturnsFirst() {
		TestProperties p1 = new TestProperties();
		TestProperties p2 = new TestProperties();
		p2.setConnector(Connector.JETTY);
		TestProperties p3 = new TestProperties();
		p3.setConnector(Connector.JDK);
		ClientHttpConnectors connectors = new ClientHttpConnectors(this.sslBundles, p1, p2, p3);
		assertThat(connectors.builder(null)).isInstanceOf(JettyClientHttpConnectorBuilder.class);
	}

	@Test
	void buildWhenHasNoConnectorPropertyReturnsDetected() {
		TestProperties properties = new TestProperties();
		ClientHttpConnectors connectors = new ClientHttpConnectors(this.sslBundles, properties);
		assertThat(connectors.builder(null)).isInstanceOf(ReactorClientHttpConnectorBuilder.class);
	}

	@Test
	void settingsWhenHasNoSettingProperties() {
		TestProperties properties = new TestProperties();
		ClientHttpConnectors connectors = new ClientHttpConnectors(this.sslBundles, properties);
		ClientHttpConnectorSettings settings = connectors.settings();
		assertThat(settings).isEqualTo(new ClientHttpConnectorSettings(null, null, null, null));
	}

	@Test
	void settingsWhenHasMultipleSettingProperties() {
		this.bundleRegistry.registerBundle("p2", mock(SslBundle.class));
		this.bundleRegistry.registerBundle("p3", mock(SslBundle.class));
		TestProperties p1 = new TestProperties();
		TestProperties p2 = new TestProperties();
		p2.setRedirects(HttpRedirects.DONT_FOLLOW);
		p2.setConnectTimeout(Duration.ofSeconds(1));
		p2.setReadTimeout(Duration.ofSeconds(2));
		p2.getSsl().setBundle("p2");
		TestProperties p3 = new TestProperties();
		p3.setRedirects(HttpRedirects.FOLLOW);
		p3.setConnectTimeout(Duration.ofSeconds(10));
		p3.setReadTimeout(Duration.ofSeconds(20));
		p3.getSsl().setBundle("p3");
		ClientHttpConnectors connectors = new ClientHttpConnectors(this.sslBundles, p1, p2, p3);
		ClientHttpConnectorSettings settings = connectors.settings();
		assertThat(settings).isEqualTo(new ClientHttpConnectorSettings(HttpRedirects.DONT_FOLLOW, Duration.ofSeconds(1),
				Duration.ofSeconds(2), this.bundleRegistry.getBundle("p2")));
	}

	static class TestProperties extends AbstractClientHttpConnectorProperties {

	}

}
