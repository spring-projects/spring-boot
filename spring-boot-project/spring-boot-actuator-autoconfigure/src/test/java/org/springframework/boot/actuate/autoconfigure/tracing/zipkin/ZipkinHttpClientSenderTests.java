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
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import zipkin2.reporter.BytesMessageSender;
import zipkin2.reporter.Encoding;
import zipkin2.reporter.HttpEndpointSupplier;
import zipkin2.reporter.HttpEndpointSuppliers;

import org.springframework.http.HttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.assertj.core.api.Assertions.assertThatIOException;

/**
 * Tests for {@link ZipkinHttpClientSender}.
 *
 * @author Moritz Halbritter
 */
class ZipkinHttpClientSenderTests extends ZipkinHttpSenderTests {

	private MockWebServer mockBackEnd;

	private String zipkinUrl;

	@Override
	@BeforeEach
	void beforeEach() throws Exception {
		this.mockBackEnd = new MockWebServer();
		this.mockBackEnd.start();
		this.zipkinUrl = this.mockBackEnd.url("/api/v2/spans").toString();
		super.beforeEach();
	}

	@Override
	void afterEach() throws IOException {
		super.afterEach();
		this.mockBackEnd.shutdown();
	}

	@Override
	BytesMessageSender createSender() {
		return createSender(Encoding.JSON, Duration.ofSeconds(10));
	}

	ZipkinHttpClientSender createSender(Encoding encoding, Duration timeout) {
		return createSender(HttpEndpointSuppliers.constantFactory(), encoding, timeout);
	}

	ZipkinHttpClientSender createSender(HttpEndpointSupplier.Factory endpointSupplierFactory, Encoding encoding,
			Duration timeout) {
		HttpClient httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
		return new ZipkinHttpClientSender(encoding, endpointSupplierFactory, this.zipkinUrl, httpClient, timeout);
	}

	@Test
	void sendShouldSendSpansToZipkin() throws IOException, InterruptedException {
		this.mockBackEnd.enqueue(new MockResponse());
		List<byte[]> encodedSpans = List.of(toByteArray("span1"), toByteArray("span2"));
		this.sender.send(encodedSpans);
		requestAssertions((request) -> {
			assertThat(request.getMethod()).isEqualTo("POST");
			assertThat(request.getHeader("Content-Type")).isEqualTo("application/json");
			assertThat(request.getBody().readUtf8()).isEqualTo("[span1,span2]");
		});
	}

	@Test
	void sendShouldSendSpansToZipkinInProto3() throws IOException, InterruptedException {
		this.mockBackEnd.enqueue(new MockResponse());
		List<byte[]> encodedSpans = List.of(toByteArray("span1"), toByteArray("span2"));
		try (BytesMessageSender sender = createSender(Encoding.PROTO3, Duration.ofSeconds(10))) {
			sender.send(encodedSpans);
		}
		requestAssertions((request) -> {
			assertThat(request.getMethod()).isEqualTo("POST");
			assertThat(request.getHeader("Content-Type")).isEqualTo("application/x-protobuf");
			assertThat(request.getBody().readUtf8()).isEqualTo("span1span2");
		});
	}

	/**
	 * This tests that a dynamic {@linkplain HttpEndpointSupplier} updates are visible to
	 * {@link HttpSender#postSpans(URI, HttpHeaders, byte[])}.
	 */
	@Test
	void sendUsesDynamicEndpoint() throws Exception {
		this.mockBackEnd.enqueue(new MockResponse());
		this.mockBackEnd.enqueue(new MockResponse());
		try (TestHttpEndpointSupplier httpEndpointSupplier = new TestHttpEndpointSupplier(this.zipkinUrl)) {
			try (BytesMessageSender sender = createSender((endpoint) -> httpEndpointSupplier, Encoding.JSON,
					Duration.ofSeconds(10))) {
				sender.send(Collections.emptyList());
				sender.send(Collections.emptyList());
			}
			assertThat(this.mockBackEnd.takeRequest().getPath()).endsWith("/1");
			assertThat(this.mockBackEnd.takeRequest().getPath()).endsWith("/2");
		}
	}

	@Test
	void sendShouldHandleHttpFailures() throws InterruptedException {
		this.mockBackEnd.enqueue(new MockResponse().setResponseCode(500));
		assertThatException().isThrownBy(() -> this.sender.send(Collections.emptyList()))
			.withMessageContaining("Expected HTTP status 2xx, got 500");
		requestAssertions((request) -> assertThat(request.getMethod()).isEqualTo("POST"));
	}

	@Test
	void sendShouldCompressData() throws IOException, InterruptedException {
		String uncompressed = "a".repeat(10000);
		// This is gzip compressed 10000 times 'a'
		byte[] compressed = Base64.getDecoder()
			.decode("H4sIAAAAAAAA/+3BMQ0AAAwDIKFLj/k3UR8NcA8AAAAAAAAAAAADUsAZfeASJwAA");
		this.mockBackEnd.enqueue(new MockResponse());
		this.sender.send(List.of(toByteArray(uncompressed)));
		requestAssertions((request) -> {
			assertThat(request.getMethod()).isEqualTo("POST");
			assertThat(request.getHeader("Content-Type")).isEqualTo("application/json");
			assertThat(request.getHeader("Content-Encoding")).isEqualTo("gzip");
			assertThat(request.getBody().readByteArray()).isEqualTo(compressed);
		});
	}

	@Test
	void shouldTimeout() throws IOException {
		try (BytesMessageSender sender = createSender(Encoding.JSON, Duration.ofMillis(1))) {
			MockResponse response = new MockResponse().setResponseCode(200).setHeadersDelay(100, TimeUnit.MILLISECONDS);
			this.mockBackEnd.enqueue(response);
			assertThatIOException().isThrownBy(() -> sender.send(Collections.emptyList()))
				.withMessageContaining("timed out");
		}
	}

	private void requestAssertions(Consumer<RecordedRequest> assertions) throws InterruptedException {
		RecordedRequest request = this.mockBackEnd.takeRequest();
		assertThat(request).satisfies(assertions);
	}

}
