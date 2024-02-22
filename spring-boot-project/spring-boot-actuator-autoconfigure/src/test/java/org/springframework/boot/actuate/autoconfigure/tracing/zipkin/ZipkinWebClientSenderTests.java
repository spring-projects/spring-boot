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
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.QueueDispatcher;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import zipkin2.reporter.BytesMessageSender;
import zipkin2.reporter.Encoding;
import zipkin2.reporter.HttpEndpointSupplier;
import zipkin2.reporter.HttpEndpointSuppliers;

import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;

/**
 * Tests for {@link ZipkinWebClientSender}.
 *
 * @author Stefan Bratanov
 */
@SuppressWarnings("removal")
class ZipkinWebClientSenderTests extends ZipkinHttpSenderTests {

	private static ClearableDispatcher dispatcher;

	private static MockWebServer mockBackEnd;

	private static String ZIPKIN_URL;

	@BeforeAll
	static void beforeAll() throws IOException {
		dispatcher = new ClearableDispatcher();
		mockBackEnd = new MockWebServer();
		mockBackEnd.setDispatcher(dispatcher);
		mockBackEnd.start();
		ZIPKIN_URL = mockBackEnd.url("/api/v2/spans").toString();
	}

	@AfterAll
	static void afterAll() throws IOException {
		mockBackEnd.shutdown();
	}

	@Override
	@BeforeEach
	void beforeEach() throws Exception {
		super.beforeEach();
		clearResponses();
		clearRequests();
	}

	@Override
	BytesMessageSender createSender() {
		return createSender(Encoding.JSON, Duration.ofSeconds(10));
	}

	ZipkinWebClientSender createSender(Encoding encoding, Duration timeout) {
		return createSender(HttpEndpointSuppliers.constantFactory(), encoding, timeout);
	}

	ZipkinWebClientSender createSender(HttpEndpointSupplier.Factory endpointSupplierFactory, Encoding encoding,
			Duration timeout) {
		WebClient webClient = WebClient.builder().build();
		return new ZipkinWebClientSender(encoding, endpointSupplierFactory, ZIPKIN_URL, webClient, timeout);
	}

	@Test
	void sendShouldSendSpansToZipkin() throws IOException, InterruptedException {
		mockBackEnd.enqueue(new MockResponse());
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
		mockBackEnd.enqueue(new MockResponse());
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
		mockBackEnd.enqueue(new MockResponse());
		mockBackEnd.enqueue(new MockResponse());
		try (HttpEndpointSupplier httpEndpointSupplier = new TestHttpEndpointSupplier(ZIPKIN_URL)) {
			try (BytesMessageSender sender = createSender((endpoint) -> httpEndpointSupplier, Encoding.JSON,
					Duration.ofSeconds(10))) {
				sender.send(Collections.emptyList());
				sender.send(Collections.emptyList());
			}
			assertThat(mockBackEnd.takeRequest().getPath()).endsWith("/1");
			assertThat(mockBackEnd.takeRequest().getPath()).endsWith("/2");
		}
	}

	@Test
	void sendShouldHandleHttpFailures() throws InterruptedException {
		mockBackEnd.enqueue(new MockResponse().setResponseCode(500));
		assertThatException().isThrownBy(() -> this.sender.send(Collections.emptyList()))
			.withMessageContaining("500 Internal Server Error");
		requestAssertions((request) -> assertThat(request.getMethod()).isEqualTo("POST"));
	}

	@Test
	void sendShouldCompressData() throws IOException, InterruptedException {
		String uncompressed = "a".repeat(10000);
		// This is gzip compressed 10000 times 'a'
		byte[] compressed = Base64.getDecoder()
			.decode("H4sIAAAAAAAA/+3BMQ0AAAwDIKFLj/k3UR8NcA8AAAAAAAAAAAADUsAZfeASJwAA");
		mockBackEnd.enqueue(new MockResponse());
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
			mockBackEnd.enqueue(response);
			assertThatException().isThrownBy(() -> sender.send(Collections.emptyList()))
				.withCauseInstanceOf(TimeoutException.class);
		}
	}

	private void requestAssertions(Consumer<RecordedRequest> assertions) throws InterruptedException {
		RecordedRequest request = mockBackEnd.takeRequest();
		assertThat(request).satisfies(assertions);
	}

	private static void clearRequests() throws InterruptedException {
		RecordedRequest request;
		do {
			request = mockBackEnd.takeRequest(0, TimeUnit.SECONDS);
		}
		while (request != null);
	}

	private static void clearResponses() {
		dispatcher.clear();
	}

	private static final class ClearableDispatcher extends QueueDispatcher {

		void clear() {
			getResponseQueue().clear();
		}

	}

}
