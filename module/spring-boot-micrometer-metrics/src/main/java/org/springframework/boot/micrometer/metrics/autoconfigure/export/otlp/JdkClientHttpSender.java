/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.micrometer.metrics.autoconfigure.export.otlp;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

import javax.net.ssl.SSLParameters;

import io.micrometer.core.ipc.http.HttpSender;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslOptions;

/**
 * {@link HttpSender} implementation using the JDK {@link HttpClient}.
 *
 * @author Moritz Halbritter
 */
class JdkClientHttpSender implements HttpSender {

	private final HttpClient httpClient;

	private final Duration timeout;

	/**
	 * Creates a new {@link JdkClientHttpSender}.
	 * @param connectTimeout the connect timeout
	 * @param timeout the request timeout
	 * @param sslBundle the SSL bundle to use for TLS configuration, or {@code null}
	 */
	JdkClientHttpSender(Duration connectTimeout, Duration timeout, @Nullable SslBundle sslBundle) {
		this.httpClient = buildHttpClient(connectTimeout, sslBundle);
		this.timeout = timeout;
	}

	private static HttpClient buildHttpClient(Duration connectTimeout, @Nullable SslBundle sslBundle) {
		HttpClient.Builder builder = HttpClient.newBuilder().connectTimeout(connectTimeout);
		if (sslBundle != null) {
			builder.sslContext(sslBundle.createSslContext());
			builder.sslParameters(asSslParameters(sslBundle));
		}
		return builder.build();
	}

	private static SSLParameters asSslParameters(SslBundle sslBundle) {
		SslOptions options = sslBundle.getOptions();
		SSLParameters parameters = new SSLParameters();
		parameters.setCipherSuites(options.getCiphers());
		parameters.setProtocols(options.getEnabledProtocols());
		return parameters;
	}

	@Override
	public Response send(Request request) throws IOException {
		HttpRequest.Builder httpRequest = HttpRequest.newBuilder()
			.uri(URI.create(request.getUrl().toString()))
			.timeout(this.timeout);
		request.getRequestHeaders().forEach(httpRequest::header);
		httpRequest.method(request.getMethod().name(), BodyPublishers.ofByteArray(request.getEntity()));
		try {
			HttpResponse<String> response = this.httpClient.send(httpRequest.build(), BodyHandlers.ofString());
			return new Response(response.statusCode(), response.body());
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IOException("HTTP request interrupted", ex);
		}
	}

}
