/*
 * Copyright 2012-2023 the original author or authors.
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import zipkin2.CheckResult;
import zipkin2.reporter.Sender;

import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link ZipkinWebClientSender}.
 *
 * @author Stefan Bratanov
 */
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
	Sender createSender() {
		return createSender(Duration.ofSeconds(10));
	}

	Sender createSender(Duration timeout) {
		WebClient webClient = WebClient.builder().build();
		return new ZipkinWebClientSender(ZIPKIN_URL, webClient, timeout);
	}

	@Test
	void checkShouldSendEmptySpanList() throws InterruptedException {
		mockBackEnd.enqueue(new MockResponse());
		assertThat(this.sender.check()).isEqualTo(CheckResult.OK);
		requestAssertions((request) -> {
			assertThat(request.getMethod()).isEqualTo("POST");
			assertThat(request.getBody().readUtf8()).isEqualTo("[]");
		});
	}

	@Test
	void checkShouldNotRaiseException() throws InterruptedException {
		mockBackEnd.enqueue(new MockResponse().setResponseCode(500));
		CheckResult result = this.sender.check();
		assertThat(result.ok()).isFalse();
		assertThat(result.error()).hasMessageContaining("500 Internal Server Error");
		requestAssertions((request) -> assertThat(request.getMethod()).isEqualTo("POST"));
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void sendSpansShouldSendSpansToZipkin(boolean async) throws IOException, InterruptedException {
		mockBackEnd.enqueue(new MockResponse());
		List<byte[]> encodedSpans = List.of(toByteArray("span1"), toByteArray("span2"));
		makeRequest(encodedSpans, async);
		requestAssertions((request) -> {
			assertThat(request.getMethod()).isEqualTo("POST");
			assertThat(request.getHeader("Content-Type")).isEqualTo("application/json");
			assertThat(request.getBody().readUtf8()).isEqualTo("[span1,span2]");
		});
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void sendSpansShouldHandleHttpFailures(boolean async) throws InterruptedException {
		mockBackEnd.enqueue(new MockResponse().setResponseCode(500));
		if (async) {
			CallbackResult callbackResult = makeAsyncRequest(Collections.emptyList());
			assertThat(callbackResult.success()).isFalse();
			assertThat(callbackResult.error()).isNotNull().hasMessageContaining("500 Internal Server Error");
		}
		else {
			assertThatThrownBy(() -> makeSyncRequest(Collections.emptyList()))
				.hasMessageContaining("500 Internal Server Error");
		}
		requestAssertions((request) -> assertThat(request.getMethod()).isEqualTo("POST"));
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void sendSpansShouldCompressData(boolean async) throws IOException, InterruptedException {
		String uncompressed = "a".repeat(10000);
		// This is gzip compressed 10000 times 'a'
		byte[] compressed = Base64.getDecoder()
			.decode("H4sIAAAAAAAA/+3BMQ0AAAwDIKFLj/k3UR8NcA8AAAAAAAAAAAADUsAZfeASJwAA");
		mockBackEnd.enqueue(new MockResponse());
		makeRequest(List.of(toByteArray(uncompressed)), async);
		requestAssertions((request) -> {
			assertThat(request.getMethod()).isEqualTo("POST");
			assertThat(request.getHeader("Content-Type")).isEqualTo("application/json");
			assertThat(request.getHeader("Content-Encoding")).isEqualTo("gzip");
			assertThat(request.getBody().readByteArray()).isEqualTo(compressed);
		});
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void shouldTimeout(boolean async) {
		Sender sender = createSender(Duration.ofMillis(1));
		MockResponse response = new MockResponse().setResponseCode(200).setHeadersDelay(100, TimeUnit.MILLISECONDS);
		mockBackEnd.enqueue(response);
		if (async) {
			CallbackResult callbackResult = makeAsyncRequest(sender, Collections.emptyList());
			assertThat(callbackResult.success()).isFalse();
			assertThat(callbackResult.error()).isNotNull().isInstanceOf(TimeoutException.class);
		}
		else {
			assertThatThrownBy(() -> makeSyncRequest(sender, Collections.emptyList()))
				.hasCauseInstanceOf(TimeoutException.class);
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

	private static class ClearableDispatcher extends QueueDispatcher {

		void clear() {
			getResponseQueue().clear();
		}

	}

}
