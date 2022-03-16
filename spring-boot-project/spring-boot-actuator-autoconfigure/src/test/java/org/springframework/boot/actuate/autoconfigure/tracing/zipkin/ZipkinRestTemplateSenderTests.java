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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import zipkin2.CheckResult;
import zipkin2.reporter.ClosedSenderException;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

/**
 * Tests for {@link ZipkinRestTemplateSender}.
 *
 * @author Moritz Halbritter
 */
class ZipkinRestTemplateSenderTests {

	private static final String ZIPKIN_URL = "http://localhost:9411/api/v2/spans";

	private MockRestServiceServer mockServer;

	private ZipkinRestTemplateSender sut;

	@BeforeEach
	void setUp() {
		RestTemplate restTemplate = new RestTemplate();
		this.mockServer = MockRestServiceServer.createServer(restTemplate);
		this.sut = new ZipkinRestTemplateSender(ZIPKIN_URL, restTemplate);
	}

	@AfterEach
	void tearDown() {
		this.mockServer.verify();
	}

	@Test
	void checkShouldSendEmptySpanList() {
		this.mockServer.expect(requestTo(ZIPKIN_URL)).andExpect(method(HttpMethod.POST))
				.andExpect(content().string("[]")).andRespond(withStatus(HttpStatus.ACCEPTED));
		assertThat(this.sut.check()).isEqualTo(CheckResult.OK);
	}

	@Test
	void checkShouldNotRaiseException() {
		this.mockServer.expect(requestTo(ZIPKIN_URL)).andExpect(method(HttpMethod.POST))
				.andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
		CheckResult result = this.sut.check();
		assertThat(result.ok()).isFalse();
		assertThat(result.error()).hasMessageContaining("500 Internal Server Error");
	}

	@Test
	void sendSpansShouldSendSpansToZipkin() throws IOException {
		this.mockServer.expect(requestTo(ZIPKIN_URL)).andExpect(method(HttpMethod.POST))
				.andExpect(content().contentType("application/json")).andExpect(content().string("[span1,span2]"))
				.andRespond(withStatus(HttpStatus.ACCEPTED));
		this.sut.sendSpans(List.of(toByteArray("span1"), toByteArray("span2"))).execute();
	}

	@Test
	void sendSpansShouldThrowOnHttpFailure() throws IOException {
		this.mockServer.expect(requestTo(ZIPKIN_URL)).andExpect(method(HttpMethod.POST))
				.andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
		assertThatThrownBy(() -> this.sut.sendSpans(List.of()).execute())
				.hasMessageContaining("500 Internal Server Error");
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
		this.mockServer.expect(requestTo(ZIPKIN_URL)).andExpect(method(HttpMethod.POST))
				.andExpect(header("Content-Encoding", "gzip")).andExpect(content().contentType("application/json"))
				.andExpect(content().bytes(compressed)).andRespond(withStatus(HttpStatus.ACCEPTED));
		this.sut.sendSpans(List.of(toByteArray(uncompressed))).execute();
	}

	private byte[] toByteArray(String input) {
		return input.getBytes(StandardCharsets.UTF_8);
	}

}
