/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.web.trace.servlet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.boot.actuate.trace.http.TraceableResponse;

/**
 * An adapter that exposes an {@link HttpServletResponse} as a {@link TraceableResponse}.
 *
 * @author Andy Wilkinson
 */
final class TraceableHttpServletResponse implements TraceableResponse {

	private final HttpServletResponse delegate;

	TraceableHttpServletResponse(HttpServletResponse response) {
		this.delegate = response;
	}

	@Override
	public int getStatus() {
		return this.delegate.getStatus();
	}

	@Override
	public Map<String, List<String>> getHeaders() {
		return extractHeaders();
	}

	private Map<String, List<String>> extractHeaders() {
		Map<String, List<String>> headers = new LinkedHashMap<>();
		for (String name : this.delegate.getHeaderNames()) {
			headers.put(name, new ArrayList<>(this.delegate.getHeaders(name)));
		}
		return headers;
	}

}
