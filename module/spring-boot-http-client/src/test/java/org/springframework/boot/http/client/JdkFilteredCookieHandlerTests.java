/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.http.client;

import java.net.CookieHandler;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JdkFilteredCookieHandler}.
 *
 * @author Phillip Webb
 */
class JdkFilteredCookieHandlerTests {

	@Test
	void getWhenMatchesResolvedHost() throws Exception {
		URI localhost = new URI("http://localhost");
		CookieHandler delegate = mock();
		Map<String, List<String>> requestHeaders = Map.of("test", List.of("test"));
		Map<String, List<String>> result = Map.of("result", List.of("result"));
		given(delegate.get(localhost, requestHeaders)).willReturn(result);
		JdkFilteredCookieHandler cookieHandler = new JdkFilteredCookieHandler(delegate,
				new UrlHostFilter(InetAddressFilter.internalAddresses()));
		assertThat(cookieHandler.get(localhost, requestHeaders)).isEqualTo(result);
	}

	@Test
	void getWhenMatchesResolvedHostAndNoDelegate() throws Exception {
		URI localhost = new URI("http://localhost");
		Map<String, List<String>> requestHeaders = Map.of("test", List.of("test"));
		JdkFilteredCookieHandler cookieHandler = new JdkFilteredCookieHandler(null,
				new UrlHostFilter(InetAddressFilter.internalAddresses()));
		assertThat(cookieHandler.get(localhost, requestHeaders)).isEmpty();
	}

	@Test
	void getWhenDoesNotMatchesResolvedHost() throws Exception {
		URI localhost = new URI("http://localhost");
		CookieHandler delegate = mock();
		Map<String, List<String>> requestHeaders = Map.of("test", List.of("test"));
		Map<String, List<String>> result = Map.of("result", List.of("result"));
		given(delegate.get(localhost, requestHeaders)).willReturn(result);
		JdkFilteredCookieHandler cookieHandler = new JdkFilteredCookieHandler(delegate,
				new UrlHostFilter(InetAddressFilter.externalAddresses()));
		assertThatExceptionOfType(FilteredHostException.class)
			.isThrownBy(() -> cookieHandler.get(localhost, requestHeaders));
	}

	@Test
	void getWhenDoesNotMatchesResolvedHostAndNoDelegate() throws Exception {
		URI localhost = new URI("http://localhost");
		Map<String, List<String>> requestHeaders = Map.of("test", List.of("test"));
		JdkFilteredCookieHandler cookieHandler = new JdkFilteredCookieHandler(null,
				new UrlHostFilter(InetAddressFilter.externalAddresses()));
		assertThatExceptionOfType(FilteredHostException.class)
			.isThrownBy(() -> cookieHandler.get(localhost, requestHeaders));
	}

	@Test
	void putDelegates() {

	}

	@Test
	void putWhenNoDelegate() {

	}

}
