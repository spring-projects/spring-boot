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
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import zipkin2.CheckResult;
import zipkin2.reporter.Sender;

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
 * @author Stefan Bratanov
 */
class ZipkinRestTemplateSenderTests extends ZipkinHttpSenderTests {

	private static final String ZIPKIN_URL = "http://localhost:9411/api/v2/spans";

	private MockRestServiceServer mockServer;

	@Override
	Sender createSut() {
		RestTemplate restTemplate = new RestTemplate();
		this.mockServer = MockRestServiceServer.createServer(restTemplate);
		return new ZipkinRestTemplateSender(ZIPKIN_URL, restTemplate);
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

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void sendSpansShouldSendSpansToZipkin(boolean async) throws IOException {
		this.mockServer.expect(requestTo(ZIPKIN_URL)).andExpect(method(HttpMethod.POST))
				.andExpect(content().contentType("application/json")).andExpect(content().string("[span1,span2]"))
				.andRespond(withStatus(HttpStatus.ACCEPTED));
		this.makeRequest(List.of(toByteArray("span1"), toByteArray("span2")), async);
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void sendSpansShouldHandleHttpFailures(boolean async) {
		this.mockServer.expect(requestTo(ZIPKIN_URL)).andExpect(method(HttpMethod.POST))
				.andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
		if (async) {
			CallbackResult callbackResult = this.makeAsyncRequest(List.of());
			assertThat(callbackResult.success()).isFalse();
			assertThat(callbackResult.error()).isNotNull().hasMessageContaining("500 Internal Server Error");
		}
		else {
			assertThatThrownBy(() -> this.makeSyncRequest(List.of())).hasMessageContaining("500 Internal Server Error");
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void sendSpansShouldCompressData(boolean async) throws IOException {
		String uncompressed = "a".repeat(10000);
		// This is gzip compressed 10000 times 'a'
		byte[] compressed = Base64.getDecoder()
				.decode("H4sIAAAAAAAA/+3BMQ0AAAwDIKFLj/k3UR8NcA8AAAAAAAAAAAADUsAZfeASJwAA");
		this.mockServer.expect(requestTo(ZIPKIN_URL)).andExpect(method(HttpMethod.POST))
				.andExpect(header("Content-Encoding", "gzip")).andExpect(content().contentType("application/json"))
				.andExpect(content().bytes(compressed)).andRespond(withStatus(HttpStatus.ACCEPTED));
		this.makeRequest(List.of(toByteArray(uncompressed)), async);
	}

}
