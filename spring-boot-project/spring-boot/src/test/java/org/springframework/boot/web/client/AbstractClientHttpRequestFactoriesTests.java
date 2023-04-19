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

package org.springframework.boot.web.client;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import org.springframework.http.client.ClientHttpRequestFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base classes for testing of {@link ClientHttpRequestFactories} with different HTTP
 * clients on the classpath.
 *
 * @param <T> the {@link ClientHttpRequestFactory} to be produced
 * @author Andy Wilkinson
 */
abstract class AbstractClientHttpRequestFactoriesTests<T extends ClientHttpRequestFactory> {

	private final Class<T> requestFactoryType;

	protected AbstractClientHttpRequestFactoriesTests(Class<T> requestFactoryType) {
		this.requestFactoryType = requestFactoryType;
	}

	@Test
	void getReturnsRequestFactoryOfExpectedType() {
		ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories
			.get(ClientHttpRequestFactorySettings.DEFAULTS);
		assertThat(requestFactory).isInstanceOf(this.requestFactoryType);
	}

	@Test
	void getOfGeneralTypeReturnsRequestFactoryOfExpectedType() {
		ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(ClientHttpRequestFactory.class,
				ClientHttpRequestFactorySettings.DEFAULTS);
		assertThat(requestFactory).isInstanceOf(this.requestFactoryType);
	}

	@Test
	void getOfSpecificTypeReturnsRequestFactoryOfExpectedType() {
		ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(this.requestFactoryType,
				ClientHttpRequestFactorySettings.DEFAULTS);
		assertThat(requestFactory).isInstanceOf(this.requestFactoryType);
	}

	@Test
	@SuppressWarnings("unchecked")
	void getReturnsRequestFactoryWithConfiguredConnectTimeout() {
		ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories
			.get(ClientHttpRequestFactorySettings.DEFAULTS.withConnectTimeout(Duration.ofSeconds(60)));
		assertThat(connectTimeout((T) requestFactory)).isEqualTo(Duration.ofSeconds(60).toMillis());
	}

	@Test
	@SuppressWarnings("unchecked")
	void getReturnsRequestFactoryWithConfiguredReadTimeout() {
		ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories
			.get(ClientHttpRequestFactorySettings.DEFAULTS.withReadTimeout(Duration.ofSeconds(120)));
		assertThat(readTimeout((T) requestFactory)).isEqualTo(Duration.ofSeconds(120).toMillis());
	}

	protected abstract long connectTimeout(T requestFactory);

	protected abstract long readTimeout(T requestFactory);

}
