/*
 * Copyright 2012-2022 the original author or authors.
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

import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests for {@link ClientHttpRequestFactories} when the simple JDK-based client is the
 * predominant HTTP client.
 *
 * @author Andy Wilkinson
 */
@ClassPathExclusions({ "httpclient5-*.jar", "okhttp-*.jar" })
class ClientHttpRequestFactoriesSimpleTests
		extends AbstractClientHttpRequestFactoriesTests<SimpleClientHttpRequestFactory> {

	ClientHttpRequestFactoriesSimpleTests() {
		super(SimpleClientHttpRequestFactory.class);
	}

	@Override
	protected long connectTimeout(SimpleClientHttpRequestFactory requestFactory) {
		return (int) ReflectionTestUtils.getField(requestFactory, "connectTimeout");
	}

	@Override
	protected long readTimeout(SimpleClientHttpRequestFactory requestFactory) {
		return (int) ReflectionTestUtils.getField(requestFactory, "readTimeout");
	}

}
