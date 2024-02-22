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
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import zipkin2.reporter.BytesMessageSender;
import zipkin2.reporter.Encoding;
import zipkin2.reporter.HttpEndpointSupplier;
import zipkin2.reporter.HttpEndpointSuppliers;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThatException;
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
@SuppressWarnings("removal")
class ZipkinRestTemplateSenderTests extends ZipkinHttpSenderTests {

	private static final String ZIPKIN_URL = "http://localhost:9411/api/v2/spans";

	private RestTemplate restTemplate;

	private MockRestServiceServer mockServer;

	@Override
	BytesMessageSender createSender() {
		this.restTemplate = new RestTemplate();
		this.mockServer = MockRestServiceServer.createServer(this.restTemplate);
		return createSender(Encoding.JSON);
	}

	BytesMessageSender createSender(Encoding encoding) {
		return createSender(HttpEndpointSuppliers.constantFactory(), encoding);
	}

	BytesMessageSender createSender(HttpEndpointSupplier.Factory endpointSupplierFactory, Encoding encoding) {
		return new ZipkinRestTemplateSender(encoding, endpointSupplierFactory, ZIPKIN_URL, this.restTemplate);
	}

	@AfterEach
	@Override
	void afterEach() throws IOException {
		super.afterEach();
		this.mockServer.verify();
	}

	@Test
	void sendShouldSendSpansToZipkin() throws IOException {
		this.mockServer.expect(requestTo(ZIPKIN_URL))
			.andExpect(method(HttpMethod.POST))
			.andExpect(content().contentType("application/json"))
			.andExpect(content().string("[span1,span2]"))
			.andRespond(withStatus(HttpStatus.ACCEPTED));
		this.sender.send(List.of(toByteArray("span1"), toByteArray("span2")));
	}

	@Test
	void sendShouldSendSpansToZipkinInProto3() throws IOException {
		this.mockServer.expect(requestTo(ZIPKIN_URL))
			.andExpect(method(HttpMethod.POST))
			.andExpect(content().contentType("application/x-protobuf"))
			.andExpect(content().string("span1span2"))
			.andRespond(withStatus(HttpStatus.ACCEPTED));
		try (BytesMessageSender sender = createSender(Encoding.PROTO3)) {
			sender.send(List.of(toByteArray("span1"), toByteArray("span2")));
		}
	}

	/**
	 * This tests that a dynamic {@linkplain HttpEndpointSupplier} updates are visible to
	 * {@link HttpSender#postSpans(URI, HttpHeaders, byte[])}.
	 */
	@Test
	void sendUsesDynamicEndpoint() throws Exception {
		this.mockServer.expect(requestTo(ZIPKIN_URL + "/1")).andRespond(withStatus(HttpStatus.ACCEPTED));
		this.mockServer.expect(requestTo(ZIPKIN_URL + "/2")).andRespond(withStatus(HttpStatus.ACCEPTED));
		try (HttpEndpointSupplier httpEndpointSupplier = new TestHttpEndpointSupplier(ZIPKIN_URL)) {
			try (BytesMessageSender sender = createSender((endpoint) -> httpEndpointSupplier, Encoding.JSON)) {
				sender.send(Collections.emptyList());
				sender.send(Collections.emptyList());
			}
		}
	}

	@Test
	void sendShouldHandleHttpFailures() {
		this.mockServer.expect(requestTo(ZIPKIN_URL))
			.andExpect(method(HttpMethod.POST))
			.andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

		assertThatException().isThrownBy(() -> this.sender.send(Collections.emptyList()))
			.withMessageContaining("500 Internal Server Error");
	}

	@Test
	void sendShouldCompressData() throws IOException {
		String uncompressed = "a".repeat(10000);
		// This is gzip compressed 10000 times 'a'
		byte[] compressed = Base64.getDecoder()
			.decode("H4sIAAAAAAAA/+3BMQ0AAAwDIKFLj/k3UR8NcA8AAAAAAAAAAAADUsAZfeASJwAA");
		this.mockServer.expect(requestTo(ZIPKIN_URL))
			.andExpect(method(HttpMethod.POST))
			.andExpect(header("Content-Encoding", "gzip"))
			.andExpect(content().contentType("application/json"))
			.andExpect(content().bytes(compressed))
			.andRespond(withStatus(HttpStatus.ACCEPTED));
		this.sender.send(List.of(toByteArray(uncompressed)));
	}

}
