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

import org.eclipse.jetty.client.HttpClient;

import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.http.client.JettyClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests for {@link ClientHttpRequestFactories} when Jetty is the predominant HTTP client.
 *
 * @author Arjen Poutsma
 */
@ClassPathExclusions({ "httpclient5-*.jar", "okhttp-*.jar" })
class ClientHttpRequestFactoriesJettyTests
		extends AbstractClientHttpRequestFactoriesTests<JettyClientHttpRequestFactory> {

	ClientHttpRequestFactoriesJettyTests() {
		super(JettyClientHttpRequestFactory.class);
	}

	@Override
	protected long connectTimeout(JettyClientHttpRequestFactory requestFactory) {
		return ((HttpClient) ReflectionTestUtils.getField(requestFactory, "httpClient")).getConnectTimeout();
	}

	@Override
	protected long readTimeout(JettyClientHttpRequestFactory requestFactory) {
		return (long) ReflectionTestUtils.getField(requestFactory, "readTimeout");
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
