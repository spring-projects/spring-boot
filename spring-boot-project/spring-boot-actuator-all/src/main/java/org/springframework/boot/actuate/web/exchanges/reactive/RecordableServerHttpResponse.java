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

package org.springframework.boot.actuate.web.exchanges.reactive;

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
		this.headers = new LinkedHashMap<>(response.getHeaders());
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
