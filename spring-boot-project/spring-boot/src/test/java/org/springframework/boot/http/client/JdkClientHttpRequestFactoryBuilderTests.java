/*
 * Copyright 2012-2024 the original author or authors.
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

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests for {@link JdkClientHttpRequestFactoryBuilder}.
 *
 * @author Phillip Webb
 */
class JdkClientHttpRequestFactoryBuilderTests
		extends AbstractClientHttpRequestFactoryBuilderTests<JdkClientHttpRequestFactory> {

	JdkClientHttpRequestFactoryBuilderTests() {
		super(JdkClientHttpRequestFactory.class, ClientHttpRequestFactoryBuilder.jdk());
	}

	@Override
	protected long connectTimeout(JdkClientHttpRequestFactory requestFactory) {
		HttpClient httpClient = (HttpClient) ReflectionTestUtils.getField(requestFactory, "httpClient");
		return httpClient.connectTimeout().get().toMillis();
	}

	@Override
	protected long readTimeout(JdkClientHttpRequestFactory requestFactory) {
		Duration readTimeout = (Duration) ReflectionTestUtils.getField(requestFactory, "readTimeout");
		return readTimeout.toMillis();
	}

}
