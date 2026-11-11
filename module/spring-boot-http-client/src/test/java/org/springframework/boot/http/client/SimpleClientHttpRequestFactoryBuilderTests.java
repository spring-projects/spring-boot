/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.http.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
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
		Object field = ReflectionTestUtils.getField(requestFactory, "connectTimeout");
		assertThat(field).isNotNull();
		return (int) field;
	}

	@Override
	protected long readTimeout(SimpleClientHttpRequestFactory requestFactory) {
		Object field = ReflectionTestUtils.getField(requestFactory, "readTimeout");
		assertThat(field).isNotNull();
		return (int) field;
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

	@Test
	@Override
	void filteredInetAddress() throws Exception {
		assertThatIllegalStateException()
			.isThrownBy(() -> getBuilder()
				.build(HttpClientSettings.defaults().withInetAddressFilter(InetAddressFilter.externalAddresses())))
			.withMessage("Simple HTTP request factory does not support InetAddress filtering");
	}

	@Test
	void throwsWhenCookieHandlingEnabled() {
		assertThatIllegalStateException().isThrownBy(() -> ClientHttpRequestFactoryBuilder.simple()
			.build(HttpClientSettings.defaults().withCookieHandling(HttpCookieHandling.ENABLE)));
	}

	@Override
	protected HttpStatus getExpectedRedirect(HttpMethod httpMethod) {
		return (httpMethod != HttpMethod.GET) ? HttpStatus.FOUND : HttpStatus.OK;
	}

}
