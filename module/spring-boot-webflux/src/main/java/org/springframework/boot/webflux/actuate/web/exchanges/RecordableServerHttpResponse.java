/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webflux.actuate.web.exchanges;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.actuate.web.exchanges.RecordableHttpResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;

/**
 * An adapter that exposes a {@link ServerHttpResponse} as a
 * {@link RecordableHttpResponse}.
 *
 * @author Andy Wilkinson
 */
class RecordableServerHttpResponse implements RecordableHttpResponse {

	private final int status;

	private final Map<String, List<String>> headers;

	RecordableServerHttpResponse(ServerHttpResponse response) {
		this.status = (response.getStatusCode() != null) ? response.getStatusCode().value() : HttpStatus.OK.value();
		Map<String, List<String>> headers = new LinkedHashMap<>();
		response.getHeaders().forEach(headers::put);
		this.headers = Collections.unmodifiableMap(headers);
	}

	@Override
	public int getStatus() {
		return this.status;
	}

	@Override
	public Map<String, List<String>> getHeaders() {
		return this.headers;
	}

}
