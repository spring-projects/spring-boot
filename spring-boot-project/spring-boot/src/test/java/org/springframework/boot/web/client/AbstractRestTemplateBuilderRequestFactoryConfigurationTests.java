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
 * Base class for tests that verify the configuration of the
 * {@link ClientHttpRequestFactory} used by {@link RestTemplateBuilder}.
 *
 * @param <T> the request factory type under test
 * @author Andy Wilkinson
 */
abstract class AbstractRestTemplateBuilderRequestFactoryConfigurationTests<T extends ClientHttpRequestFactory> {

	private final Class<? extends ClientHttpRequestFactory> factoryType;

	private final RestTemplateBuilder builder = new RestTemplateBuilder();

	protected AbstractRestTemplateBuilderRequestFactoryConfigurationTests(Class<T> factoryType) {
		this.factoryType = factoryType;
	}

	@Test
	@SuppressWarnings("unchecked")
	void connectTimeoutCanBeConfiguredOnFactory() {
		ClientHttpRequestFactory requestFactory = this.builder.requestFactory(this.factoryType)
			.setConnectTimeout(Duration.ofMillis(1234))
			.build()
			.getRequestFactory();
		assertThat(connectTimeout((T) requestFactory)).isEqualTo(1234);
	}

	@Test
	@SuppressWarnings("unchecked")
	void readTimeoutCanBeConfiguredOnFactory() {
		ClientHttpRequestFactory requestFactory = this.builder.requestFactory(this.factoryType)
			.setReadTimeout(Duration.ofMillis(1234))
			.build()
			.getRequestFactory();
		assertThat(readTimeout((T) requestFactory)).isEqualTo(1234);
	}

	@Test
	@SuppressWarnings("unchecked")
	void connectTimeoutCanBeConfiguredOnDetectedFactory() {
		ClientHttpRequestFactory requestFactory = this.builder.setConnectTimeout(Duration.ofMillis(1234))
			.build()
			.getRequestFactory();
		assertThat(connectTimeout((T) requestFactory)).isEqualTo(1234);
	}

	@Test
	@SuppressWarnings("unchecked")
	void readTimeoutCanBeConfiguredOnDetectedFactory() {
		ClientHttpRequestFactory requestFactory = this.builder.setReadTimeout(Duration.ofMillis(1234))
			.build()
			.getRequestFactory();
		assertThat(readTimeout((T) requestFactory)).isEqualTo(1234);
	}

	protected abstract long connectTimeout(T requestFactory);

	protected abstract long readTimeout(T requestFactory);

}
