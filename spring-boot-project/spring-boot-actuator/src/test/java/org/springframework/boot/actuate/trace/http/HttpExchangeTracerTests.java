/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.trace.http;

import java.net.URI;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import org.springframework.boot.actuate.trace.http.HttpTrace.Request;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HttpExchangeTracer}.
 *
 * @author Andy Wilkinson
 */
public class HttpExchangeTracerTests {

	@Test
	public void methodIsIncluded() {
		HttpTrace trace = new HttpExchangeTracer(EnumSet.noneOf(Include.class))
				.receivedRequest(createRequest());
		Request request = trace.getRequest();
		assertThat(request.getMethod()).isEqualTo("GET");
	}

	@Test
	public void uriIsIncluded() {
		HttpTrace trace = new HttpExchangeTracer(EnumSet.noneOf(Include.class))
				.receivedRequest(createRequest());
		Request request = trace.getRequest();
		assertThat(request.getUri()).isEqualTo(URI.create("https://api.example.com"));
	}

	@Test
	public void remoteAddressIsNotIncludedByDefault() {
		HttpTrace trace = new HttpExchangeTracer(EnumSet.noneOf(Include.class))
				.receivedRequest(createRequest());
		Request request = trace.getRequest();
		assertThat(request.getRemoteAddress()).isNull();
	}

	@Test
	public void remoteAddressCanBeIncluded() {
		HttpTrace trace = new HttpExchangeTracer(EnumSet.of(Include.REMOTE_ADDRESS))
				.receivedRequest(createRequest());
		Request request = trace.getRequest();
		assertThat(request.getRemoteAddress()).isEqualTo("127.0.0.1");
	}

	@Test
	public void requestHeadersAreNotIncludedByDefault() {
		HttpTrace trace = new HttpExchangeTracer(EnumSet.noneOf(Include.class))
				.receivedRequest(createRequest());
		Request request = trace.getRequest();
		assertThat(request.getHeaders()).isEmpty();
	}

	@Test
	public void requestHeadersCanBeIncluded() {
		HttpTrace trace = new HttpExchangeTracer(EnumSet.of(Include.REQUEST_HEADERS))
				.receivedRequest(createRequest());
		Request request = trace.getRequest();
		assertThat(request.getHeaders()).containsOnlyKeys(HttpHeaders.ACCEPT);
	}

	@Test
	public void requestHeadersCanBeCustomized() {
		MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
		headers.add("to-remove", "test");
		headers.add("test", "value");
		HttpTrace trace = new RequestHeadersFilterHttpExchangeTracer()
				.receivedRequest(createRequest(headers));
		Request request = trace.getRequest();
		assertThat(request.getHeaders()).containsOnlyKeys("test", "to-add");
		assertThat(request.getHeaders().get("test")).containsExactly("value");
		assertThat(request.getHeaders().get("to-add")).containsExactly("42");
	}

	@Test
	public void authorizationHeaderIsNotIncludedByDefault() {
		HttpTrace trace = new HttpExchangeTracer(EnumSet.of(Include.REQUEST_HEADERS))
				.receivedRequest(createRequest(Collections.singletonMap(
						HttpHeaders.AUTHORIZATION, Arrays.asList("secret"))));
		Request request = trace.getRequest();
		assertThat(request.getHeaders()).isEmpty();
	}

	@Test
	public void mixedCaseAuthorizationHeaderIsNotIncludedByDefault() {
		HttpTrace trace = new HttpExchangeTracer(EnumSet.of(Include.REQUEST_HEADERS))
				.receivedRequest(createRequest(Collections.singletonMap(
						mixedCase(HttpHeaders.AUTHORIZATION), Arrays.asList("secret"))));
		Request request = trace.getRequest();
		assertThat(request.getHeaders()).isEmpty();
	}

	@Test
	public void authorizationHeaderCanBeIncluded() {
		HttpTrace trace = new HttpExchangeTracer(
				EnumSet.of(Include.REQUEST_HEADERS, Include.AUTHORIZATION_HEADER))
						.receivedRequest(createRequest(Collections.singletonMap(
								HttpHeaders.AUTHORIZATION, Arrays.asList("secret"))));
		Request request = trace.getRequest();
		assertThat(request.getHeaders()).containsOnlyKeys(HttpHeaders.AUTHORIZATION);
	}

	@Test
	public void mixedCaseAuthorizationHeaderCanBeIncluded() {
		HttpTrace trace = new HttpExchangeTracer(
				EnumSet.of(Include.REQUEST_HEADERS, Include.AUTHORIZATION_HEADER))
						.receivedRequest(createRequest(Collections.singletonMap(
								mixedCase(HttpHeaders.AUTHORIZATION),
								Arrays.asList("secret"))));
		Request request = trace.getRequest();
		assertThat(request.getHeaders())
				.containsOnlyKeys(mixedCase(HttpHeaders.AUTHORIZATION));
	}

	@Test
	public void cookieHeaderIsNotIncludedByDefault() {
		HttpTrace trace = new HttpExchangeTracer(EnumSet.of(Include.REQUEST_HEADERS))
				.receivedRequest(createRequest(Collections
						.singletonMap(HttpHeaders.COOKIE, Arrays.asList("test=test"))));
		Request request = trace.getRequest();
		assertThat(request.getHeaders()).isEmpty();
	}

	@Test
	public void mixedCaseCookieHeaderIsNotIncludedByDefault() {
		HttpTrace trace = new HttpExchangeTracer(EnumSet.of(Include.REQUEST_HEADERS))
				.receivedRequest(createRequest(Collections.singletonMap(
						mixedCase(HttpHeaders.COOKIE), Arrays.asList("value"))));
		Request request = trace.getRequest();
		assertThat(request.getHeaders()).isEmpty();
	}

	@Test
	public void cookieHeaderCanBeIncluded() {
		HttpTrace trace = new HttpExchangeTracer(
				EnumSet.of(Include.REQUEST_HEADERS, Include.COOKIE_HEADERS))
						.receivedRequest(createRequest(Collections.singletonMap(
								HttpHeaders.COOKIE, Arrays.asList("value"))));
		Request request = trace.getRequest();
		assertThat(request.getHeaders()).containsOnlyKeys(HttpHeaders.COOKIE);
	}

	@Test
	public void mixedCaseCookieHeaderCanBeIncluded() {
		HttpTrace trace = new HttpExchangeTracer(
				EnumSet.of(Include.REQUEST_HEADERS, Include.COOKIE_HEADERS))
						.receivedRequest(createRequest(Collections.singletonMap(
								mixedCase(HttpHeaders.COOKIE), Arrays.asList("value"))));
		Request request = trace.getRequest();
		assertThat(request.getHeaders()).containsOnlyKeys(mixedCase(HttpHeaders.COOKIE));
	}

	@Test
	public void statusIsIncluded() {
		HttpTrace trace = new HttpTrace(createRequest());
		new HttpExchangeTracer(EnumSet.noneOf(Include.class)).sendingResponse(trace,
				createResponse(), null, null);
		assertThat(trace.getResponse().getStatus()).isEqualTo(204);
	}

	@Test
	public void responseHeadersAreNotIncludedByDefault() {
		HttpTrace trace = new HttpTrace(createRequest());
		new HttpExchangeTracer(EnumSet.noneOf(Include.class)).sendingResponse(trace,
				createResponse(), null, null);
		assertThat(trace.getResponse().getHeaders()).isEmpty();
	}

	@Test
	public void responseHeadersCanBeIncluded() {
		HttpTrace trace = new HttpTrace(createRequest());
		new HttpExchangeTracer(EnumSet.of(Include.RESPONSE_HEADERS))
				.sendingResponse(trace, createResponse(), null, null);
		assertThat(trace.getResponse().getHeaders())
				.containsOnlyKeys(HttpHeaders.CONTENT_TYPE);
	}

	@Test
	public void setCookieHeaderIsNotIncludedByDefault() {
		HttpTrace trace = new HttpTrace(createRequest());
		new HttpExchangeTracer(EnumSet.of(Include.RESPONSE_HEADERS)).sendingResponse(
				trace, createResponse(Collections.singletonMap(HttpHeaders.SET_COOKIE,
						Arrays.asList("test=test"))),
				null, null);
		assertThat(trace.getResponse().getHeaders()).isEmpty();
	}

	@Test
	public void mixedCaseSetCookieHeaderIsNotIncludedByDefault() {
		HttpTrace trace = new HttpTrace(createRequest());
		new HttpExchangeTracer(EnumSet.of(Include.RESPONSE_HEADERS)).sendingResponse(
				trace,
				createResponse(Collections.singletonMap(mixedCase(HttpHeaders.SET_COOKIE),
						Arrays.asList("test=test"))),
				null, null);
		assertThat(trace.getResponse().getHeaders()).isEmpty();
	}

	@Test
	public void setCookieHeaderCanBeIncluded() {
		HttpTrace trace = new HttpTrace(createRequest());
		new HttpExchangeTracer(
				EnumSet.of(Include.RESPONSE_HEADERS, Include.COOKIE_HEADERS))
						.sendingResponse(trace,
								createResponse(
										Collections.singletonMap(HttpHeaders.SET_COOKIE,
												Arrays.asList("test=test"))),
								null, null);
		assertThat(trace.getResponse().getHeaders())
				.containsOnlyKeys(HttpHeaders.SET_COOKIE);
	}

	@Test
	public void mixedCaseSetCookieHeaderCanBeIncluded() {
		HttpTrace trace = new HttpTrace(createRequest());
		new HttpExchangeTracer(
				EnumSet.of(Include.RESPONSE_HEADERS, Include.COOKIE_HEADERS))
						.sendingResponse(trace,
								createResponse(Collections.singletonMap(
										mixedCase(HttpHeaders.SET_COOKIE),
										Arrays.asList("test=test"))),
								null, null);
		assertThat(trace.getResponse().getHeaders())
				.containsOnlyKeys(mixedCase(HttpHeaders.SET_COOKIE));
	}

	@Test
	public void principalIsNotIncludedByDefault() {
		HttpTrace trace = new HttpTrace(createRequest());
		new HttpExchangeTracer(EnumSet.noneOf(Include.class)).sendingResponse(trace,
				createResponse(), this::createPrincipal, null);
		assertThat(trace.getPrincipal()).isNull();
	}

	@Test
	public void principalCanBeIncluded() {
		HttpTrace trace = new HttpTrace(createRequest());
		new HttpExchangeTracer(EnumSet.of(Include.PRINCIPAL)).sendingResponse(trace,
				createResponse(), this::createPrincipal, null);
		assertThat(trace.getPrincipal()).isNotNull();
		assertThat(trace.getPrincipal().getName()).isEqualTo("alice");
	}

	@Test
	public void sessionIdIsNotIncludedByDefault() {
		HttpTrace trace = new HttpTrace(createRequest());
		new HttpExchangeTracer(EnumSet.noneOf(Include.class)).sendingResponse(trace,
				createResponse(), null, () -> "sessionId");
		assertThat(trace.getSession()).isNull();
	}

	@Test
	public void sessionIdCanBeIncluded() {
		HttpTrace trace = new HttpTrace(createRequest());
		new HttpExchangeTracer(EnumSet.of(Include.SESSION_ID)).sendingResponse(trace,
				createResponse(), null, () -> "sessionId");
		assertThat(trace.getSession()).isNotNull();
		assertThat(trace.getSession().getId()).isEqualTo("sessionId");
	}

	@Test
	public void timeTakenIsNotIncludedByDefault() {
		HttpTrace trace = new HttpTrace(createRequest());
		new HttpExchangeTracer(EnumSet.noneOf(Include.class)).sendingResponse(trace,
				createResponse(), null, null);
		assertThat(trace.getTimeTaken()).isNull();
	}

	@Test
	public void timeTakenCanBeIncluded() {
		HttpTrace trace = new HttpTrace(createRequest());
		new HttpExchangeTracer(EnumSet.of(Include.TIME_TAKEN)).sendingResponse(trace,
				createResponse(), null, null);
		assertThat(trace.getTimeTaken()).isNotNull();
	}

	private TraceableRequest createRequest() {
		return createRequest(Collections.singletonMap(HttpHeaders.ACCEPT,
				Arrays.asList("application/json")));
	}

	private TraceableRequest createRequest(Map<String, List<String>> headers) {
		TraceableRequest request = mock(TraceableRequest.class);
		given(request.getMethod()).willReturn("GET");
		given(request.getRemoteAddress()).willReturn("127.0.0.1");
		given(request.getHeaders()).willReturn(new HashMap<>(headers));
		given(request.getUri()).willReturn(URI.create("https://api.example.com"));
		return request;
	}

	private TraceableResponse createResponse() {
		return createResponse(Collections.singletonMap(HttpHeaders.CONTENT_TYPE,
				Arrays.asList("application/json")));
	}

	private TraceableResponse createResponse(Map<String, List<String>> headers) {
		TraceableResponse response = mock(TraceableResponse.class);
		given(response.getStatus()).willReturn(204);
		given(response.getHeaders()).willReturn(new HashMap<>(headers));
		return response;
	}

	private Principal createPrincipal() {
		Principal principal = mock(Principal.class);
		given(principal.getName()).willReturn("alice");
		return principal;
	}

	private String mixedCase(String input) {
		StringBuilder output = new StringBuilder();
		for (int i = 0; i < input.length(); i++) {
			output.append((i % 2 != 0) ? Character.toUpperCase(input.charAt(i))
					: Character.toLowerCase(input.charAt(i)));
		}
		return output.toString();
	}

	private static class RequestHeadersFilterHttpExchangeTracer
			extends HttpExchangeTracer {

		RequestHeadersFilterHttpExchangeTracer() {
			super(EnumSet.of(Include.REQUEST_HEADERS));
		}

		@Override
		protected void postProcessRequestHeaders(Map<String, List<String>> headers) {
			headers.remove("to-remove");
			headers.putIfAbsent("to-add", Collections.singletonList("42"));
		}

	}

}
