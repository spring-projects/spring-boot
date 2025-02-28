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

package org.springframework.boot.web.client;

import java.time.Duration;

import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;

import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests for {@link ClientHttpRequestFactories} when Reactor Netty is the predominant HTTP
 * client.
 *
 * @author Andy Wilkinson
 */
@ClassPathExclusions({ "httpclient5-*.jar", "jetty-client-*.jar" })
@SuppressWarnings("removal")
class ClientHttpRequestFactoriesReactorTests
		extends AbstractClientHttpRequestFactoriesTests<ReactorClientHttpRequestFactory> {

	ClientHttpRequestFactoriesReactorTests() {
		super(ReactorClientHttpRequestFactory.class);
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

	@Override
	protected boolean supportsSettingConnectTimeout() {
		return true;
	}

	@Override
	protected boolean supportsSettingReadTimeout() {
		return true;
	}

}
