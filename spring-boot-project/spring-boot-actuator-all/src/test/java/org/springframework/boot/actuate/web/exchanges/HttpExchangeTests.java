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

package org.springframework.boot.actuate.web.exchanges;

import java.net.URI;
import java.security.Principal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link HttpExchange}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class HttpExchangeTests {

	private static final Map<String, List<String>> AUTHORIZATION_HEADER = Map.of(HttpHeaders.AUTHORIZATION,
			Arrays.asList("secret"));

	private static final Map<String, List<String>> COOKIE_HEADER = Map.of(HttpHeaders.COOKIE,
			Arrays.asList("test=test"));

	private static final Map<String, List<String>> SET_COOKIE_HEADER = Map.of(HttpHeaders.SET_COOKIE,
			Arrays.asList("test=test"));

	private static final Supplier<Principal> NO_PRINCIPAL = () -> null;

	private static final Supplier<String> NO_SESSION_ID = () -> null;

	private static final Supplier<Principal> WITH_PRINCIPAL = () -> {
		Principal principal = mock(Principal.class);
		given(principal.getName()).willReturn("alice");
		return principal;
	};

	private static final Supplier<String> WITH_SESSION_ID = () -> "JSESSION_123";

	@Test
	void getTimestampReturnsTimestamp() {
		Instant now = Instant.now();
		Clock clock = Clock.fixed(now, ZoneId.systemDefault());
		HttpExchange exchange = HttpExchange.start(clock, createRequest())
			.finish(createResponse(), NO_PRINCIPAL, NO_SESSION_ID, Include.defaultIncludes());
		assertThat(exchange.getTimestamp()).isEqualTo(now);
	}

	@Test
	void getRequestUriReturnsUri() {
		HttpExchange exchange = HttpExchange.start(createRequest())
			.finish(createResponse(), NO_PRINCIPAL, NO_SESSION_ID, Include.defaultIncludes());
		assertThat(exchange.getRequest().getUri()).isEqualTo(URI.create("https://api.example.com"));
	}

	@Test
	void getRequestRemoteAddressWhenUsingDefaultIncludesReturnsNull() {
		HttpExchange exchange = HttpExchange.start(createRequest())
			.finish(createResponse(), NO_PRINCIPAL, NO_SESSION_ID, Include.defaultIncludes());
		assertThat(exchange.getRequest().getRemoteAddress()).isNull();
	}

	@Test
	void getRequestRemoteAddressWhenIncludedReturnsRemoteAddress() {
		HttpExchange exchange = HttpExchange.start(createRequest())
			.finish(createResponse(), NO_PRINCIPAL, NO_SESSION_ID, Include.REMOTE_ADDRESS);
		assertThat(exchange.getRequest().getRemoteAddress()).isEqualTo("127.0.0.1");
	}

	@Test
	void getRequestMethodReturnsHttpMethod() {
		HttpExchange exchange = HttpExchange.start(createRequest())
			.finish(createResponse(), NO_PRINCIPAL, NO_SESSION_ID, Include.defaultIncludes());
		assertThat(exchange.getRequest().getMethod()).isEqualTo("GET");
	}

	@Test
	void getRequestHeadersWhenUsingDefaultIncludesReturnsHeaders() {
		HttpExchange exchange = HttpExchange.start(createRequest())
			.finish(createResponse(), NO_PRINCIPAL, NO_SESSION_ID, Include.defaultIncludes());
		assertThat(exchange.getRequest().getHeaders()).containsOnlyKeys(HttpHeaders.ACCEPT);
	}

	@Test
	void getRequestHeadersWhenIncludedReturnsHeaders() {
		HttpExchange exchange = HttpExchange.start(createRequest())
			.finish(createResponse(), NO_PRINCIPAL, NO_SESSION_ID, Include.REQUEST_HEADERS);
		assertThat(exchange.getRequest().getHeaders()).containsOnlyKeys(HttpHeaders.ACCEPT);
	}

	@Test
	void getRequestHeadersWhenNotIncludedReturnsEmptyHeaders() {
		HttpExchange exchange = HttpExchange.start(createRequest())
			.finish(createResponse(), NO_PRINCIPAL, NO_SESSION_ID);
		assertThat(exchange.getRequest().getHeaders()).isEmpty();
	}

	@Test
	void getRequestHeadersWhenUsingDefaultIncludesFiltersAuthorizeHeader() {
		HttpExchange exchange = HttpExchange.start(createRequest(AUTHORIZATION_HEADER))
			.finish(createResponse(), NO_PRINCIPAL, NO_SESSION_ID, Include.defaultIncludes());
		assertThat(exchange.getRequest().getHeaders()).isEmpty();
	}

	@Test
	void getRequestHeadersWhenIncludesAuthorizationHeaderReturnsHeaders() {
		HttpExchange exchange = HttpExchange.start(createRequest(AUTHORIZATION_HEADER))
			.finish(createResponse(), NO_PRINCIPAL, NO_SESSION_ID, Include.REQUEST_HEADERS,
					Include.AUTHORIZATION_HEADER);
		assertThat(exchange.getRequest().getHeaders()).containsOnlyKeys(HttpHeaders.AUTHORIZATION);
	}

	@Test
	void getRequestHeadersWhenIncludesAuthorizationHeaderAndInDifferentCaseReturnsHeaders() {
		HttpExchange exchange = HttpExchange.start(createRequest(mixedCase(AUTHORIZATION_HEADER)))
			.finish(createResponse(), NO_PRINCIPAL, NO_SESSION_ID, Include.REQUEST_HEADERS,
					Include.AUTHORIZATION_HEADER);
		assertThat(exchange.getRequest().getHeaders()).containsOnlyKeys(mixedCase(HttpHeaders.AUTHORIZATION));
	}

	@Test
	void getRequestHeadersWhenUsingDefaultIncludesFiltersCookieHeader() {
		HttpExchange exchange = HttpExchange.start(createRequest(COOKIE_HEADER))
			.finish(createResponse(), NO_PRINCIPAL, NO_SESSION_ID, Include.defaultIncludes());
		assertThat(exchange.getRequest().getHeaders()).isEmpty();
	}

	@Test
	void getRequestHeadersWhenIncludesCookieHeaderReturnsHeaders() {
		HttpExchange exchange = HttpExchange.start(createRequest(COOKIE_HEADER))
			.finish(createResponse(), NO_PRINCIPAL, NO_SESSION_ID, Include.REQUEST_HEADERS, Include.COOKIE_HEADERS);
		assertThat(exchange.getRequest().getHeaders()).containsOnlyKeys(HttpHeaders.COOKIE);
	}

	@Test
	void getRequestHeadersWhenIncludesCookieHeaderAndInDifferentCaseReturnsHeaders() {
		HttpExchange exchange = HttpExchange.start(createRequest(mixedCase(COOKIE_HEADER)))
			.finish(createResponse(), NO_PRINCIPAL, NO_SESSION_ID, Include.REQUEST_HEADERS, Include.COOKIE_HEADERS);
		assertThat(exchange.getRequest().getHeaders()).containsOnlyKeys(mixedCase(HttpHeaders.COOKIE));
	}

	@Test
	void getResponseStatusReturnsStatus() {
		HttpExchange exchange = HttpExchange.start(createRequest())
			.finish(createResponse(), NO_PRINCIPAL, NO_SESSION_ID, Include.REMOTE_ADDRESS);
		assertThat(exchange.getResponse().getStatus()).isEqualTo(204);
	}

	@Test
	void getResponseHeadersWhenUsingDefaultIncludesReturnsHeaders() {
		HttpExchange exchange = HttpExchange.start(createRequest())
			.finish(createResponse(), NO_PRINCIPAL, NO_SESSION_ID, Include.defaultIncludes());
		assertThat(exchange.getResponse().getHeaders()).containsOnlyKeys(HttpHeaders.CONTENT_TYPE);
	}

	@Test
	void getResponseHeadersWhenNotIncludedReturnsEmptyHeaders() {
		HttpExchange exchange = HttpExchange.start(createRequest())
			.finish(createResponse(), NO_PRINCIPAL, NO_SESSION_ID);
		assertThat(exchange.getResponse().getHeaders()).isEmpty();
	}

	@Test
	void getResponseHeadersIncludedReturnsHeaders() {
		HttpExchange exchange = HttpExchange.start(createRequest())
			.finish(createResponse(), NO_PRINCIPAL, NO_SESSION_ID, Include.RESPONSE_HEADERS);
		assertThat(exchange.getResponse().getHeaders()).containsOnlyKeys(HttpHeaders.CONTENT_TYPE);
	}

	@Test
	void getResponseHeadersWhenUsingDefaultIncludesFiltersSetCookieHeader() {
		HttpExchange exchange = HttpExchange.start(createRequest())
			.finish(createResponse(SET_COOKIE_HEADER), NO_PRINCIPAL, NO_SESSION_ID, Include.defaultIncludes());
		assertThat(exchange.getResponse().getHeaders()).isEmpty();
	}

	@Test
	void getResponseHeadersWhenIncludesCookieHeaderReturnsHeaders() {
		HttpExchange exchange = HttpExchange.start(createRequest())
			.finish(createResponse(SET_COOKIE_HEADER), NO_PRINCIPAL, NO_SESSION_ID, Include.RESPONSE_HEADERS,
					Include.COOKIE_HEADERS);
		assertThat(exchange.getResponse().getHeaders()).containsKey(HttpHeaders.SET_COOKIE);
	}

	@Test
	void getResponseHeadersWhenIncludesCookieHeaderAndInDifferentCaseReturnsHeaders() {
		HttpExchange exchange = HttpExchange.start(createRequest())
			.finish(createResponse(mixedCase(SET_COOKIE_HEADER)), NO_PRINCIPAL, NO_SESSION_ID, Include.RESPONSE_HEADERS,
					Include.COOKIE_HEADERS);
		assertThat(exchange.getResponse().getHeaders()).containsKey(mixedCase(HttpHeaders.SET_COOKIE));
	}

	@Test
	void getPrincipalWhenUsingDefaultIncludesReturnsNull() {
		HttpExchange exchange = HttpExchange.start(createRequest())
			.finish(createResponse(), WITH_PRINCIPAL, NO_SESSION_ID, Include.defaultIncludes());
		assertThat(exchange.getPrincipal()).isNull();
	}

	@Test
	void getPrincipalWhenIncludesPrincipalReturnsPrincipal() {
		HttpExchange exchange = HttpExchange.start(createRequest())
			.finish(createResponse(), WITH_PRINCIPAL, NO_SESSION_ID, Include.PRINCIPAL);
		assertThat(exchange.getPrincipal()).isNotNull();
		assertThat(exchange.getPrincipal().getName()).isEqualTo("alice");
	}

	@Test
	void getSessionIdWhenUsingDefaultIncludesReturnsNull() {
		HttpExchange exchange = HttpExchange.start(createRequest())
			.finish(createResponse(), NO_PRINCIPAL, WITH_SESSION_ID, Include.defaultIncludes());
		assertThat(exchange.getSession()).isNull();
	}

	@Test
	void getSessionIdWhenIncludesSessionReturnsSessionId() {
		HttpExchange exchange = HttpExchange.start(createRequest())
			.finish(createResponse(), NO_PRINCIPAL, WITH_SESSION_ID, Include.SESSION_ID);
		assertThat(exchange.getSession()).isNotNull();
		assertThat(exchange.getSession().getId()).isEqualTo("JSESSION_123");
	}

	@Test
	void getTimeTakenWhenUsingDefaultIncludesReturnsTimeTaken() {
		HttpExchange exchange = HttpExchange.start(createRequest())
			.finish(createResponse(), NO_PRINCIPAL, NO_SESSION_ID, Include.defaultIncludes());
		assertThat(exchange.getTimeTaken()).isNotNull();
	}

	@Test
	void getTimeTakenWhenNotIncludedReturnsNull() {
		HttpExchange exchange = HttpExchange.start(createRequest())
			.finish(createResponse(), NO_PRINCIPAL, NO_SESSION_ID);
		assertThat(exchange.getTimeTaken()).isNull();
	}

	@Test
	void getTimeTakenWhenIncludesTimeTakenReturnsTimeTaken() {
		Duration duration = Duration.ofSeconds(1);
		Clock startClock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
		Clock finishClock = Clock.offset(startClock, duration);
		HttpExchange exchange = HttpExchange.start(startClock, createRequest())
			.finish(finishClock, createResponse(), NO_PRINCIPAL, NO_SESSION_ID, Include.TIME_TAKEN);
		assertThat(exchange.getTimeTaken()).isEqualTo(duration);
	}

	@Test
	void defaultIncludes() {
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		requestHeaders.set(HttpHeaders.COOKIE, "value");
		requestHeaders.set(HttpHeaders.AUTHORIZATION, "secret");
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HttpHeaders.SET_COOKIE, "test=test");
		responseHeaders.setContentLength(0);
		HttpExchange exchange = HttpExchange.start(createRequest(requestHeaders))
			.finish(createResponse(responseHeaders), NO_PRINCIPAL, NO_SESSION_ID, Include.defaultIncludes());
		assertThat(exchange.getTimeTaken()).isNotNull();
		assertThat(exchange.getPrincipal()).isNull();
		assertThat(exchange.getSession()).isNull();
		assertThat(exchange.getTimestamp()).isNotNull();
		assertThat(exchange.getRequest().getMethod()).isEqualTo("GET");
		assertThat(exchange.getRequest().getRemoteAddress()).isNull();
		assertThat(exchange.getResponse().getStatus()).isEqualTo(204);
		assertThat(exchange.getRequest().getHeaders()).containsOnlyKeys(HttpHeaders.ACCEPT);
		assertThat(exchange.getResponse().getHeaders()).containsOnlyKeys(HttpHeaders.CONTENT_LENGTH);
	}

	private RecordableHttpRequest createRequest() {
		return createRequest(Collections.singletonMap(HttpHeaders.ACCEPT, Arrays.asList("application/json")));
	}

	private RecordableHttpRequest createRequest(Map<String, List<String>> headers) {
		RecordableHttpRequest request = mock(RecordableHttpRequest.class);
		given(request.getMethod()).willReturn("GET");
		given(request.getUri()).willReturn(URI.create("https://api.example.com"));
		given(request.getHeaders()).willReturn(new HashMap<>(headers));
		given(request.getRemoteAddress()).willReturn("127.0.0.1");
		return request;
	}

	private RecordableHttpResponse createResponse() {
		return createResponse(Collections.singletonMap(HttpHeaders.CONTENT_TYPE, Arrays.asList("application/json")));
	}

	private RecordableHttpResponse createResponse(Map<String, List<String>> headers) {
		RecordableHttpResponse response = mock(RecordableHttpResponse.class);
		given(response.getStatus()).willReturn(204);
		given(response.getHeaders()).willReturn(new HashMap<>(headers));
		return response;
	}

	private Map<String, List<String>> mixedCase(Map<String, List<String>> headers) {
		Map<String, List<String>> result = new LinkedHashMap<>();
		headers.forEach((key, value) -> result.put(mixedCase(key), value));
		return result;
	}

	private String mixedCase(String input) {
		StringBuilder output = new StringBuilder();
		for (int i = 0; i < input.length(); i++) {
			char ch = input.charAt(i);
			output.append((i % 2 != 0) ? Character.toUpperCase(ch) : Character.toLowerCase(ch));
		}
		return output.toString();
	}

}
