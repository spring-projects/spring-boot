/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.servlet.actuate.web.exchanges;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.boot.actuate.web.exchanges.RecordableHttpResponse;

/**
 * An adapter that exposes an {@link HttpServletResponse} as a
 * {@link RecordableHttpResponse}.
 *
 * @author Andy Wilkinson
 */
final class RecordableServletHttpResponse implements RecordableHttpResponse {

	private final HttpServletResponse delegate;

	private final int status;

	RecordableServletHttpResponse(HttpServletResponse response, int status) {
		this.delegate = response;
		this.status = status;
	}

	@Override
	public int getStatus() {
		return this.status;
	}

	@Override
	public Map<String, List<String>> getHeaders() {
		Map<String, List<String>> headers = new LinkedHashMap<>();
		for (String name : this.delegate.getHeaderNames()) {
			headers.put(name, new ArrayList<>(this.delegate.getHeaders(name)));
		}
		return headers;
	}

}
