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

package org.springframework.boot.actuate.web.exchanges.servlet;

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

	/**
	 * Constructs a new RecordableServletHttpResponse with the specified
	 * HttpServletResponse and status code.
	 * @param response the HttpServletResponse object to delegate to
	 * @param status the HTTP status code to set
	 */
	RecordableServletHttpResponse(HttpServletResponse response, int status) {
		this.delegate = response;
		this.status = status;
	}

	/**
	 * Returns the status code of the HTTP response.
	 * @return the status code of the HTTP response
	 */
	@Override
	public int getStatus() {
		return this.status;
	}

	/**
	 * Returns the headers of the HTTP response.
	 * @return a map containing the headers of the HTTP response, where the key is the
	 * header name and the value is a list of header values
	 */
	@Override
	public Map<String, List<String>> getHeaders() {
		Map<String, List<String>> headers = new LinkedHashMap<>();
		for (String name : this.delegate.getHeaderNames()) {
			headers.put(name, new ArrayList<>(this.delegate.getHeaders(name)));
		}
		return headers;
	}

}
