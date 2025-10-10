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

package org.springframework.boot.http.client;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.Test;

import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JdkClientHttpRequestFactoryBuilder} and {@link JdkHttpClientBuilder}.
 *
 * @author Phillip Webb
 */
class JdkClientHttpRequestFactoryBuilderTests
		extends AbstractClientHttpRequestFactoryBuilderTests<JdkClientHttpRequestFactory> {

	JdkClientHttpRequestFactoryBuilderTests() {
		super(JdkClientHttpRequestFactory.class, ClientHttpRequestFactoryBuilder.jdk());
	}

	@Test
	void withCustomizers() {
		TestCustomizer<HttpClient.Builder> httpClientCustomizer1 = new TestCustomizer<>();
		TestCustomizer<HttpClient.Builder> httpClientCustomizer2 = new TestCustomizer<>();
		ClientHttpRequestFactoryBuilder.jdk()
			.withHttpClientCustomizer(httpClientCustomizer1)
			.withHttpClientCustomizer(httpClientCustomizer2)
			.build();
		httpClientCustomizer1.assertCalled();
		httpClientCustomizer2.assertCalled();
	}

	@Test
	void withExecutor() {
		Executor executor = new SimpleAsyncTaskExecutor();
		JdkClientHttpRequestFactory factory = ClientHttpRequestFactoryBuilder.jdk().withExecutor(executor).build();
		HttpClient httpClient = (HttpClient) ReflectionTestUtils.getField(factory, "httpClient");
		assertThat(httpClient).isNotNull();
		assertThat(httpClient.executor()).containsSame(executor);
	}

	@Test
	void with() {
		TestCustomizer<HttpClient.Builder> customizer = new TestCustomizer<>();
		ClientHttpRequestFactoryBuilder.jdk().with((builder) -> builder.withHttpClientCustomizer(customizer)).build();
		customizer.assertCalled();
	}

	@Override
	protected long connectTimeout(JdkClientHttpRequestFactory requestFactory) {
		HttpClient httpClient = (HttpClient) ReflectionTestUtils.getField(requestFactory, "httpClient");
		assertThat(httpClient).isNotNull();
		return httpClient.connectTimeout().get().toMillis();
	}

	@Override
	protected long readTimeout(JdkClientHttpRequestFactory requestFactory) {
		Duration readTimeout = (Duration) ReflectionTestUtils.getField(requestFactory, "readTimeout");
		assertThat(readTimeout).isNotNull();
		return readTimeout.toMillis();
	}

}
