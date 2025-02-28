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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link SimpleClientHttpRequestFactoryBuilder}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class SimpleClientHttpRequestFactoryBuilderTests
		extends AbstractClientHttpRequestFactoryBuilderTests<SimpleClientHttpRequestFactory> {

	SimpleClientHttpRequestFactoryBuilderTests() {
		super(SimpleClientHttpRequestFactory.class, ClientHttpRequestFactoryBuilder.simple());
	}

	@Override
	protected long connectTimeout(SimpleClientHttpRequestFactory requestFactory) {
		return (int) ReflectionTestUtils.getField(requestFactory, "connectTimeout");
	}

	@Override
	protected long readTimeout(SimpleClientHttpRequestFactory requestFactory) {
		return (int) ReflectionTestUtils.getField(requestFactory, "readTimeout");
	}

	@Override
	void connectWithSslBundleAndOptionsMismatch(String httpMethod) throws Exception {
		assertThatIllegalStateException().isThrownBy(() -> super.connectWithSslBundleAndOptionsMismatch(httpMethod))
			.withMessage("SSL Options cannot be specified with Java connections");
	}

	@ParameterizedTest
	@ValueSource(strings = { "GET", "POST", "PUT", "DELETE" })
	@Override
	void redirectDefault(String httpMethod) throws Exception {
		super.redirectDefault(httpMethod);
	}

	@ParameterizedTest
	@ValueSource(strings = { "GET", "POST", "PUT", "DELETE" })
	@Override
	void redirectFollow(String httpMethod) throws Exception {
		super.redirectFollow(httpMethod);
	}

	@ParameterizedTest
	@ValueSource(strings = { "GET", "POST", "PUT", "DELETE" })
	@Override
	void redirectDontFollow(String httpMethod) throws Exception {
		super.redirectDontFollow(httpMethod);
	}

	@Override
	protected HttpStatus getExpectedRedirect(HttpMethod httpMethod) {
		return (httpMethod != HttpMethod.GET) ? HttpStatus.FOUND : HttpStatus.OK;
	}

}
