/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webflux.actuate.web.exchanges;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.actuate.web.exchanges.RecordableHttpRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;

/**
 * A {@link RecordableHttpRequest} backed by a {@link ServerHttpRequest}.
 *
 * @author Andy Wilkinson
 */
class RecordableServerHttpRequest implements RecordableHttpRequest {

	private final String method;

	private final HttpHeaders headers;

	private final URI uri;

	private final @Nullable String remoteAddress;

	RecordableServerHttpRequest(ServerHttpRequest request) {
		this.method = request.getMethod().name();
		this.headers = request.getHeaders();
		this.uri = request.getURI();
		this.remoteAddress = getRemoteAddress(request);
	}

	private static @Nullable String getRemoteAddress(ServerHttpRequest request) {
		InetSocketAddress remoteAddress = request.getRemoteAddress();
		InetAddress address = (remoteAddress != null) ? remoteAddress.getAddress() : null;
		return (address != null) ? address.toString() : null;
	}

	@Override
	public String getMethod() {
		return this.method;
	}

	@Override
	public URI getUri() {
		return this.uri;
	}

	@Override
	public Map<String, List<String>> getHeaders() {
		Map<String, List<String>> headers = new LinkedHashMap<>();
		this.headers.forEach(headers::put);
		return Collections.unmodifiableMap(headers);
	}

	@Override
	public @Nullable String getRemoteAddress() {
		return this.remoteAddress;
	}

}
