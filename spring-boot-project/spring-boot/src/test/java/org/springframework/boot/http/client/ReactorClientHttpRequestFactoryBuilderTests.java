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

import java.time.Duration;

import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;

import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests for {@link ReactorClientHttpRequestFactoryBuilder}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class ReactorClientHttpRequestFactoryBuilderTests
		extends AbstractClientHttpRequestFactoryBuilderTests<ReactorClientHttpRequestFactory> {

	ReactorClientHttpRequestFactoryBuilderTests() {
		super(ReactorClientHttpRequestFactory.class, ClientHttpRequestFactoryBuilder.reactor());
	}

	@Override
	protected long connectTimeout(ReactorClientHttpRequestFactory requestFactory) {
		return (int) ((HttpClient) ReflectionTestUtils.getField(requestFactory, "httpClient")).configuration()
			.options()
			.get(ChannelOption.CONNECT_TIMEOUT_MILLIS);
	}

	@Override
	protected long readTimeout(ReactorClientHttpRequestFactory requestFactory) {
		return ((Duration) ReflectionTestUtils.getField(requestFactory, "readTimeout")).toMillis();
	}

}
