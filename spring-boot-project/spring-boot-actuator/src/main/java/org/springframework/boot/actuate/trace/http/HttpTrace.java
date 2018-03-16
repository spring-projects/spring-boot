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
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.util.StringUtils;

/**
 * A trace event for handling of an HTTP request and response exchange. Can be used for
 * analyzing contextual information such as HTTP headers.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public final class HttpTrace {

	private final Instant timestamp = Instant.now();

	private volatile Principal principal;

	private volatile Session session;

	private final Request request;

	private volatile Response response;

	private volatile Long timeTaken;

	HttpTrace(TraceableRequest request) {
		this.request = new Request(request);
	}

	public Instant getTimestamp() {
		return this.timestamp;
	}

	void setPrincipal(java.security.Principal principal) {
		if (principal != null) {
			this.principal = new Principal(principal.getName());
		}
	}

	public Principal getPrincipal() {
		return this.principal;
	}

	public Session getSession() {
		return this.session;
	}

	void setSessionId(String sessionId) {
		if (StringUtils.hasText(sessionId)) {
			this.session = new Session(sessionId);
		}
	}

	public Request getRequest() {
		return this.request;
	}

	public Response getResponse() {
		return this.response;
	}

	void setResponse(Response response) {
		this.response = response;
	}

	public Long getTimeTaken() {
		return this.timeTaken;
	}

	void setTimeTaken(long timeTaken) {
		this.timeTaken = timeTaken;
	}

	/**
	 * Trace of an HTTP request.
	 */
	public static final class Request {

		private final String method;

		private final URI uri;

		private final Map<String, List<String>> headers;

		private final String remoteAddress;

		private Request(TraceableRequest request) {
			this.method = request.getMethod();
			this.uri = request.getUri();
			this.headers = request.getHeaders();
			this.remoteAddress = request.getRemoteAddress();
		}

		public String getMethod() {
			return this.method;
		}

		public URI getUri() {
			return this.uri;
		}

		public Map<String, List<String>> getHeaders() {
			return this.headers;
		}

		public String getRemoteAddress() {
			return this.remoteAddress;
		}

	}

	/**
	 * Trace of an HTTP response.
	 */
	public static final class Response {

		private final int status;

		private final Map<String, List<String>> headers;

		Response(TraceableResponse response) {
			this.status = response.getStatus();
			this.headers = response.getHeaders();
		}

		public int getStatus() {
			return this.status;
		}

		public Map<String, List<String>> getHeaders() {
			return this.headers;
		}

	}

	/**
	 * Session associated with an HTTP request-response exchange.
	 */
	public static final class Session {

		private final String id;

		private Session(String id) {
			this.id = id;
		}

		public String getId() {
			return this.id;
		}

	}

	/**
	 * Principal associated with an HTTP request-response exchange.
	 */
	public static final class Principal {

		private final String name;

		private Principal(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

	}

}
