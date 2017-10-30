/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.web.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.logging.Log;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

/**
 * {@link ClientHttpRequestInterceptor} that logs request and response bodies.
 *
 * @author Mark Hobson
 * @since 2.0.0
 */
public class LoggingInterceptor implements ClientHttpRequestInterceptor {

	private final Log log;

	public LoggingInterceptor(Log log) {
		this.log = log;
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body,
			ClientHttpRequestExecution execution) throws IOException {
		if (this.log.isDebugEnabled()) {
			this.log.debug(String.format("Request: %s %s %s", request.getMethod(),
					request.getURI(), new String(body, StandardCharsets.UTF_8)));
		}

		ClientHttpResponse response = execution.execute(request, body);

		if (this.log.isDebugEnabled()) {
			this.log.debug(String.format("Response: %s %s", response.getStatusCode().value(),
					StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8)));
		}

		return response;
	}
}
