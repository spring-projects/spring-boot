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

import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.core5.function.Resolver;
import org.apache.hc.core5.http.io.SocketConfig;

import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests for {@link ClientHttpRequestFactories} when Apache Http Components is the
 * predominant HTTP client.
 *
 * @author Andy Wilkinson
 */
class ClientHttpRequestFactoriesHttpComponentsTests
		extends AbstractClientHttpRequestFactoriesTests<HttpComponentsClientHttpRequestFactory> {

	ClientHttpRequestFactoriesHttpComponentsTests() {
		super(HttpComponentsClientHttpRequestFactory.class);
	}

	@Override
	protected long connectTimeout(HttpComponentsClientHttpRequestFactory requestFactory) {
		return (int) ReflectionTestUtils.getField(requestFactory, "connectTimeout");
	}

	@Override
	@SuppressWarnings("unchecked")
	protected long readTimeout(HttpComponentsClientHttpRequestFactory requestFactory) {
		HttpClient httpClient = requestFactory.getHttpClient();
		Object connectionManager = ReflectionTestUtils.getField(httpClient, "connManager");
		SocketConfig socketConfig = ((Resolver<HttpRoute, SocketConfig>) ReflectionTestUtils.getField(connectionManager,
				"socketConfigResolver"))
			.resolve(null);
		return socketConfig.getSoTimeout().toMilliseconds();
	}

}
