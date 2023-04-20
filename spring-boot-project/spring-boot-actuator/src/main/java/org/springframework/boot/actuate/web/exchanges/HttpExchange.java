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

package org.springframework.boot.actuate.web.exchanges;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.http.HttpHeaders;

/**
 * An HTTP request and response exchange. Can be used for analyzing contextual information
 * such as HTTP headers. Data from this class will be exposed by the
 * {@link HttpExchangesEndpoint}, usually as JSON.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.0.0
 */
public final class HttpExchange {

	private final Instant timestamp;

	private final Request request;

	private final Response response;

	private final Principal principal;

	private final Session session;

	private final Duration timeTaken;

	/**
	 * Primarily for use by {@link HttpExchangeRepository} implementations when recreating
	 * an exchange from a persistent store.
	 * @param timestamp the instant that the exchange started
	 * @param request the request
	 * @param response the response
	 * @param principal the principal
	 * @param session the session
	 * @param timeTaken the total time taken
	 */
	public HttpExchange(Instant timestamp, Request request, Response response, Principal principal, Session session,
			Duration timeTaken) {
		this.timestamp = timestamp;
		this.request = request;
		this.response = response;
		this.principal = principal;
		this.session = session;
		this.timeTaken = timeTaken;
	}

	/**
	 * Returns the instant that the exchange started.
	 * @return the start timestamp
	 */
	public Instant getTimestamp() {
		return this.timestamp;
	}

	/**
	 * Returns the request that started the exchange.
	 * @return the request.
	 */
	public Request getRequest() {
		return this.request;
	}

	/**
	 * Returns the response that completed the exchange.
	 * @return the response.
	 */
	public Response getResponse() {
		return this.response;
	}

	/**
	 * Returns the principal.
	 * @return the request
	 */
	public Principal getPrincipal() {
		return this.principal;
	}

	/**
	 * Returns the session details.
	 * @return the session
	 */
	public Session getSession() {
		return this.session;
	}

	/**
	 * Returns the total time taken for the exchange.
	 * @return the total time taken
	 */
	public Duration getTimeTaken() {
		return this.timeTaken;
	}

	/**
	 * Start a new {@link Started} from the given source request.
	 * @param request the recordable HTTP request
	 * @return an in-progress request
	 */
	public static Started start(RecordableHttpRequest request) {
		return start(Clock.systemUTC(), request);
	}

	/**
	 * Start a new {@link Started} from the given source request.
	 * @param clock the clock to use
	 * @param request the recordable HTTP request
	 * @return an in-progress request
	 */
	public static Started start(Clock clock, RecordableHttpRequest request) {
		return new Started(clock, request);
	}

	/**
	 * A started request that when {@link #finish finished} will return a new
	 * {@link HttpExchange} instance.
	 */
	public static final class Started {

		private final Instant timestamp;

		private final RecordableHttpRequest request;

		private Started(Clock clock, RecordableHttpRequest request) {
			this.timestamp = Instant.now(clock);
			this.request = request;
		}

		/**
		 * Finish the request and return a new {@link HttpExchange} instance.
		 * @param response the recordable HTTP response
		 * @param principalSupplier a supplier to provide the principal
		 * @param sessionIdSupplier a supplier to provide the session ID
		 * @param includes the options to include
		 * @return a new {@link HttpExchange} instance
		 */
		public HttpExchange finish(RecordableHttpResponse response, Supplier<java.security.Principal> principalSupplier,
				Supplier<String> sessionIdSupplier, Include... includes) {
			return finish(Clock.systemUTC(), response, principalSupplier, sessionIdSupplier, includes);
		}

		/**
		 * Finish the request and return a new {@link HttpExchange} instance.
		 * @param clock the clock to use
		 * @param response the recordable HTTP response
		 * @param principalSupplier a supplier to provide the principal
		 * @param sessionIdSupplier a supplier to provide the session ID
		 * @param includes the options to include
		 * @return a new {@link HttpExchange} instance
		 */
		public HttpExchange finish(Clock clock, RecordableHttpResponse response,
				Supplier<java.security.Principal> principalSupplier, Supplier<String> sessionIdSupplier,
				Include... includes) {
			return finish(clock, response, principalSupplier, sessionIdSupplier,
					new HashSet<>(Arrays.asList(includes)));
		}

		/**
		 * Finish the request and return a new {@link HttpExchange} instance.
		 * @param response the recordable HTTP response
		 * @param principalSupplier a supplier to provide the principal
		 * @param sessionIdSupplier a supplier to provide the session ID
		 * @param includes the options to include
		 * @return a new {@link HttpExchange} instance
		 */
		public HttpExchange finish(RecordableHttpResponse response, Supplier<java.security.Principal> principalSupplier,
				Supplier<String> sessionIdSupplier, Set<Include> includes) {
			return finish(Clock.systemUTC(), response, principalSupplier, sessionIdSupplier, includes);
		}

		/**
		 * Finish the request and return a new {@link HttpExchange} instance.
		 * @param clock the clock to use
		 * @param response the recordable HTTP response
		 * @param principalSupplier a supplier to provide the principal
		 * @param sessionIdSupplier a supplier to provide the session ID
		 * @param includes the options to include
		 * @return a new {@link HttpExchange} instance
		 */
		public HttpExchange finish(Clock clock, RecordableHttpResponse response,
				Supplier<java.security.Principal> principalSupplier, Supplier<String> sessionIdSupplier,
				Set<Include> includes) {
			Request exchangeRequest = new Request(this.request, includes);
			Response exchangeResponse = new Response(response, includes);
			Principal principal = getIfIncluded(includes, Include.PRINCIPAL, () -> Principal.from(principalSupplier));
			Session session = getIfIncluded(includes, Include.SESSION_ID, () -> Session.from(sessionIdSupplier));
			Duration duration = getIfIncluded(includes, Include.TIME_TAKEN,
					() -> Duration.between(this.timestamp, Instant.now(clock)));
			return new HttpExchange(this.timestamp, exchangeRequest, exchangeResponse, principal, session, duration);
		}

		private <T> T getIfIncluded(Set<Include> includes, Include include, Supplier<T> supplier) {
			return (includes.contains(include)) ? supplier.get() : null;
		}

	}

	/**
	 * The request that started the exchange.
	 */
	public static final class Request {

		private final URI uri;

		private final String remoteAddress;

		private final String method;

		private final Map<String, List<String>> headers;

		private Request(RecordableHttpRequest request, Set<Include> includes) {
			this.uri = request.getUri();
			this.remoteAddress = (includes.contains(Include.REMOTE_ADDRESS)) ? request.getRemoteAddress() : null;
			this.method = request.getMethod();
			this.headers = Collections.unmodifiableMap(filterHeaders(request.getHeaders(), includes));
		}

		/**
		 * Creates a fully-configured {@code Request} instance. Primarily for use by
		 * {@link HttpExchangeRepository} implementations when recreating a request from a
		 * persistent store.
		 * @param uri the URI of the request
		 * @param remoteAddress remote address from which the request was sent, if known
		 * @param method the HTTP method of the request
		 * @param headers the request headers
		 */
		public Request(URI uri, String remoteAddress, String method, Map<String, List<String>> headers) {
			this.uri = uri;
			this.remoteAddress = remoteAddress;
			this.method = method;
			this.headers = Collections.unmodifiableMap(new LinkedHashMap<>(headers));
		}

		private Map<String, List<String>> filterHeaders(Map<String, List<String>> headers, Set<Include> includes) {
			HeadersFilter filter = new HeadersFilter(includes, Include.REQUEST_HEADERS);
			filter.excludeUnless(HttpHeaders.COOKIE, Include.COOKIE_HEADERS);
			filter.excludeUnless(HttpHeaders.AUTHORIZATION, Include.AUTHORIZATION_HEADER);
			return filter.apply(headers);
		}

		/**
		 * Return the HTTP method requested.
		 * @return the HTTP method
		 */
		public String getMethod() {
			return this.method;
		}

		/**
		 * Return the URI requested.
		 * @return the URI
		 */
		public URI getUri() {
			return this.uri;
		}

		/**
		 * Return the request headers.
		 * @return the request headers
		 */
		public Map<String, List<String>> getHeaders() {
			return this.headers;
		}

		/**
		 * Return the remote address that made the request.
		 * @return the remote address
		 */
		public String getRemoteAddress() {
			return this.remoteAddress;
		}

	}

	/**
	 * The response that finished the exchange.
	 */
	public static final class Response {

		private final int status;

		private final Map<String, List<String>> headers;

		private Response(RecordableHttpResponse request, Set<Include> includes) {
			this.status = request.getStatus();
			this.headers = Collections.unmodifiableMap(filterHeaders(request.getHeaders(), includes));
		}

		/**
		 * Creates a fully-configured {@code Response} instance. Primarily for use by
		 * {@link HttpExchangeRepository} implementations when recreating a response from
		 * a persistent store.
		 * @param status the status of the response
		 * @param headers the response headers
		 */
		public Response(int status, Map<String, List<String>> headers) {
			this.status = status;
			this.headers = Collections.unmodifiableMap(new LinkedHashMap<>(headers));
		}

		private Map<String, List<String>> filterHeaders(Map<String, List<String>> headers, Set<Include> includes) {
			HeadersFilter filter = new HeadersFilter(includes, Include.RESPONSE_HEADERS);
			filter.excludeUnless(HttpHeaders.SET_COOKIE, Include.COOKIE_HEADERS);
			return filter.apply(headers);
		}

		/**
		 * Return the status code of the response.
		 * @return the response status code
		 */
		public int getStatus() {
			return this.status;
		}

		/**
		 * Return the response headers.
		 * @return the headers
		 */
		public Map<String, List<String>> getHeaders() {
			return this.headers;
		}

	}

	/**
	 * The session associated with the exchange.
	 */
	public static final class Session {

		private final String id;

		/**
		 * Creates a {@code Session}. Primarily for use by {@link HttpExchangeRepository}
		 * implementations when recreating a session from a persistent store.
		 * @param id the session id
		 */
		public Session(String id) {
			this.id = id;
		}

		/**
		 * Return the ID of the session.
		 * @return the session ID
		 */
		public String getId() {
			return this.id;
		}

		static Session from(Supplier<String> sessionIdSupplier) {
			String id = sessionIdSupplier.get();
			return (id != null) ? new Session(id) : null;
		}

	}

	/**
	 * Principal associated with an HTTP request-response exchange.
	 */
	public static final class Principal {

		private final String name;

		/**
		 * Creates a {@code Principal}. Primarily for use by {@link Principal}
		 * implementations when recreating a response from a persistent store.
		 * @param name the name of the principal
		 */
		public Principal(String name) {
			this.name = name;
		}

		/**
		 * Return the name of the principal.
		 * @return the principal name
		 */
		public String getName() {
			return this.name;
		}

		static Principal from(Supplier<java.security.Principal> principalSupplier) {
			java.security.Principal principal = principalSupplier.get();
			return (principal != null) ? new Principal(principal.getName()) : null;
		}

	}

	/**
	 * Utility class used to filter headers.
	 */
	private static class HeadersFilter {

		private final Set<Include> includes;

		private final Include requiredInclude;

		private final Set<String> filteredHeaderNames;

		HeadersFilter(Set<Include> includes, Include requiredInclude) {
			this.includes = includes;
			this.requiredInclude = requiredInclude;
			this.filteredHeaderNames = new HashSet<>();
		}

		void excludeUnless(String header, Include exception) {
			if (!this.includes.contains(exception)) {
				this.filteredHeaderNames.add(header.toLowerCase());
			}
		}

		Map<String, List<String>> apply(Map<String, List<String>> headers) {
			if (!this.includes.contains(this.requiredInclude)) {
				return Collections.emptyMap();
			}
			Map<String, List<String>> filtered = new LinkedHashMap<>();
			headers.forEach((name, value) -> {
				if (!this.filteredHeaderNames.contains(name.toLowerCase())) {
					filtered.put(name, value);
				}
			});
			return filtered;
		}

	}

}
