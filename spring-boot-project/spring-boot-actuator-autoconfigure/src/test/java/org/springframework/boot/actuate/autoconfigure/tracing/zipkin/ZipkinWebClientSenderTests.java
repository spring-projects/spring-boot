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

package org.springframework.boot.actuate.autoconfigure.tracing.zipkin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import zipkin2.CheckResult;
import zipkin2.reporter.ClosedSenderException;

import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link ZipkinWebClientSender}.
 *
 * @author Stefan Bratanov
 */
class ZipkinWebClientSenderTests {

	public static MockWebServer mockBackEnd;

	private static String ZIPKIN_URL;

	private ZipkinWebClientSender sut;

	@BeforeAll
	static void beforeAll() throws IOException {
		mockBackEnd = new MockWebServer();
		mockBackEnd.start();
		ZIPKIN_URL = "http://localhost:%s/api/v2/spans".formatted(mockBackEnd.getPort());
	}

	@AfterAll
	static void tearDown() throws IOException {
		mockBackEnd.shutdown();
	}

	@BeforeEach
	void setUp() {
		WebClient webClient = WebClient.builder().build();
		this.sut = new ZipkinWebClientSender(ZIPKIN_URL, webClient);
	}

	@Test
	void checkShouldSendEmptySpanList() {
		mockBackEnd.enqueue(new MockResponse());
		assertThat(this.sut.check()).isEqualTo(CheckResult.OK);

		requestAssertions((request) -> {
			assertThat(request.getMethod()).isEqualTo("POST");
			assertThat(request.getBody().readUtf8()).isEqualTo("[]");
		});
	}

	@Test
	void checkShouldNotRaiseExceptionBecauseItIsNotBlocking() {
		mockBackEnd.enqueue(new MockResponse().setResponseCode(500));
		CheckResult result = this.sut.check();
		assertThat(result.ok()).isTrue();

		requestAssertions((request) -> assertThat(request.getMethod()).isEqualTo("POST"));
	}

	@Test
	void sendSpansShouldSendSpansToZipkin() throws IOException {
		mockBackEnd.enqueue(new MockResponse());
		this.sut.sendSpans(List.of(toByteArray("span1"), toByteArray("span2"))).execute();

		requestAssertions((request) -> {
			assertThat(request.getMethod()).isEqualTo("POST");
			assertThat(request.getHeader("Content-Type")).isEqualTo("application/json");
			assertThat(request.getBody().readUtf8()).isEqualTo("[span1,span2]");
		});
	}

	@Test
	void sendSpansShouldNotThrowOnHttpFailureBecauseItIsNonBlocking() throws IOException {
		mockBackEnd.enqueue(new MockResponse().setResponseCode(500));
		this.sut.sendSpans(List.of()).execute();

		requestAssertions((request) -> assertThat(request.getMethod()).isEqualTo("POST"));
	}

	@Test
	void sendSpansShouldThrowIfCloseWasCalled() throws IOException {
		this.sut.close();
		assertThatThrownBy(() -> this.sut.sendSpans(List.of())).isInstanceOf(ClosedSenderException.class);
	}

	@Test
	void sendSpansShouldCompressData() throws IOException {
		String uncompressed = "a".repeat(10000);
		// This is gzip compressed 10000 times 'a'
		byte[] compressed = Base64.getDecoder()
				.decode("H4sIAAAAAAAA/+3BMQ0AAAwDIKFLj/k3UR8NcA8AAAAAAAAAAAADUsAZfeASJwAA");

		mockBackEnd.enqueue(new MockResponse());
		this.sut.sendSpans(List.of(toByteArray(uncompressed))).execute();

		requestAssertions((request) -> {
			assertThat(request.getMethod()).isEqualTo("POST");
			assertThat(request.getHeader("Content-Type")).isEqualTo("application/json");
			assertThat(request.getHeader("Content-Encoding")).isEqualTo("gzip");
			assertThat(request.getBody().readByteArray()).isEqualTo(compressed);
		});

	}

	private void requestAssertions(Consumer<RecordedRequest> assertions) {
		try {
			RecordedRequest request = mockBackEnd.takeRequest();
			assertThat(request).satisfies(assertions);
		}
		catch (InterruptedException ex) {
			Assertions.fail(ex);
		}
	}

	private byte[] toByteArray(String input) {
		return input.getBytes(StandardCharsets.UTF_8);
	}

}
