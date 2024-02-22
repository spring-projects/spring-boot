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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

import zipkin2.reporter.Encoding;
import zipkin2.reporter.HttpEndpointSupplier.Factory;

import org.springframework.http.HttpHeaders;

/**
 * A {@link HttpSender} which uses the JDK {@link HttpClient} for HTTP communication.
 *
 * @author Moritz Halbritter
 */
class ZipkinHttpClientSender extends HttpSender {

	private final HttpClient httpClient;

	private final Duration readTimeout;

	ZipkinHttpClientSender(Encoding encoding, Factory endpointSupplierFactory, String endpoint, HttpClient httpClient,
			Duration readTimeout) {
		super(encoding, endpointSupplierFactory, endpoint);
		this.httpClient = httpClient;
		this.readTimeout = readTimeout;
	}

	@Override
	void postSpans(URI endpoint, HttpHeaders headers, byte[] body) throws IOException {
		Builder request = HttpRequest.newBuilder()
			.POST(BodyPublishers.ofByteArray(body))
			.uri(endpoint)
			.timeout(this.readTimeout);
		headers.forEach((name, values) -> values.forEach((value) -> request.header(name, value)));
		try {
			HttpResponse<Void> response = this.httpClient.send(request.build(), BodyHandlers.discarding());
			if (response.statusCode() / 100 != 2) {
				throw new IOException("Expected HTTP status 2xx, got %d".formatted(response.statusCode()));
			}
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IOException("Got interrupted while sending spans", ex);
		}
	}

}
