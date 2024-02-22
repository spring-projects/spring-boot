/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.tracing.zipkin;

import java.net.URI;
import java.time.Duration;

import zipkin2.reporter.Encoding;
import zipkin2.reporter.HttpEndpointSupplier.Factory;

import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * An {@link HttpSender} which uses {@link WebClient} for HTTP communication.
 *
 * @author Stefan Bratanov
 * @author Moritz Halbritter
 */
@Deprecated(since = "3.3.0", forRemoval = true)
class ZipkinWebClientSender extends HttpSender {

	private final WebClient webClient;

	private final Duration timeout;

	ZipkinWebClientSender(Encoding encoding, Factory endpointSupplierFactory, String endpoint, WebClient webClient,
			Duration timeout) {
		super(encoding, endpointSupplierFactory, endpoint);
		this.webClient = webClient;
		this.timeout = timeout;
	}

	@Override
	void postSpans(URI endpoint, HttpHeaders headers, byte[] body) {
		this.webClient.post()
			.uri(endpoint)
			.headers((h) -> h.addAll(headers))
			.bodyValue(body)
			.retrieve()
			.toBodilessEntity()
			.timeout(this.timeout)
			.block();
	}

}
