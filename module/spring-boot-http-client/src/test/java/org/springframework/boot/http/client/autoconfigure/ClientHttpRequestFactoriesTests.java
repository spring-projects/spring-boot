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

package org.springframework.boot.http.client.autoconfigure;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.http.client.HttpComponentsClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpRedirects;
import org.springframework.boot.http.client.JettyClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.autoconfigure.AbstractHttpRequestFactoryProperties.Factory;
import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ClientHttpRequestFactories}
 *
 * @author Phillip Webb
 */
class ClientHttpRequestFactoriesTests {

	private final DefaultSslBundleRegistry bundleRegistry = new DefaultSslBundleRegistry();

	private ObjectFactory<SslBundles> sslBundles = () -> this.bundleRegistry;

	@Test
	void builderWhenHasFactoryPropertyReturnsFirst() {
		TestProperties p1 = new TestProperties();
		TestProperties p2 = new TestProperties();
		p2.setFactory(Factory.JETTY);
		TestProperties p3 = new TestProperties();
		p3.setFactory(Factory.REACTOR);
		ClientHttpRequestFactories factories = new ClientHttpRequestFactories(this.sslBundles, p1, p2, p3);
		assertThat(factories.builder(null)).isInstanceOf(JettyClientHttpRequestFactoryBuilder.class);
	}

	@Test
	void buildWhenHasNoFactoryPropertyReturnsDetected() {
		TestProperties properties = new TestProperties();
		ClientHttpRequestFactories factories = new ClientHttpRequestFactories(this.sslBundles, properties);
		assertThat(factories.builder(null)).isInstanceOf(HttpComponentsClientHttpRequestFactoryBuilder.class);
	}

	@Test
	void settingsWhenHasNoSettingProperties() {
		TestProperties properties = new TestProperties();
		ClientHttpRequestFactories factories = new ClientHttpRequestFactories(this.sslBundles, properties);
		ClientHttpRequestFactorySettings settings = factories.settings();
		assertThat(settings).isEqualTo(new ClientHttpRequestFactorySettings(null, null, null, null));
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
		ClientHttpRequestFactories factories = new ClientHttpRequestFactories(this.sslBundles, p1, p2, p3);
		ClientHttpRequestFactorySettings settings = factories.settings();
		assertThat(settings).isEqualTo(new ClientHttpRequestFactorySettings(HttpRedirects.DONT_FOLLOW,
				Duration.ofSeconds(1), Duration.ofSeconds(2), this.bundleRegistry.getBundle("p2")));
	}

	static class TestProperties extends AbstractHttpRequestFactoryProperties {

	}

}
