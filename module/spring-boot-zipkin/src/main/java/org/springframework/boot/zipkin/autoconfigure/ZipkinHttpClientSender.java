/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.zipkin.autoconfigure;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Map;

import zipkin2.reporter.Encoding;
import zipkin2.reporter.HttpEndpointSupplier.Factory;

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
	void postSpans(URI endpoint, Map<String, String> headers, byte[] body) throws IOException {
		Builder request = HttpRequest.newBuilder()
			.POST(BodyPublishers.ofByteArray(body))
			.uri(endpoint)
			.timeout(this.readTimeout);
		headers.forEach((name, value) -> request.header(name, value));
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
