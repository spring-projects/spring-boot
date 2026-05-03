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
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.ipc.http.HttpSender.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;

/**
 * Tests for {@link JdkClientHttpSender}.
 *
 * @author Moritz Halbritter
 */
class JdkClientHttpSenderTests {

	private MockWebServer mockWebServer;

	private JdkClientHttpSender sender;

	@BeforeEach
	void setUp() throws IOException {
		this.mockWebServer = new MockWebServer();
		this.mockWebServer.start();
		this.sender = new JdkClientHttpSender(Duration.ofSeconds(5), Duration.ofSeconds(5), null);
	}

	@AfterEach
	void tearDown() throws IOException {
		this.mockWebServer.shutdown();
	}

	@Test
	void sendShouldSendGetRequest() throws Throwable {
		this.mockWebServer.enqueue(new MockResponse().setBody("response-body").setResponseCode(200));
		String url = this.mockWebServer.url("/test").toString();
		Response response = this.sender.get(url).send();
		assertThat(response.code()).isEqualTo(200);
		assertThat(response.body()).isEqualTo("response-body");
		RecordedRequest request = this.mockWebServer.takeRequest();
		assertThat(request.getMethod()).isEqualTo("GET");
		assertThat(request.getPath()).isEqualTo("/test");
	}

	@Test
	void sendShouldSendPostRequest() throws Throwable {
		this.mockWebServer.enqueue(new MockResponse().setResponseCode(200));
		String url = this.mockWebServer.url("/test").toString();
		Response response = this.sender.post(url).withJsonContent("{\"key\":\"value\"}").send();
		assertThat(response.code()).isEqualTo(200);
		RecordedRequest request = this.mockWebServer.takeRequest();
		assertThat(request.getMethod()).isEqualTo("POST");
		assertThat(request.getHeader("Content-Type")).isEqualTo("application/json");
		assertThat(request.getBody().readUtf8()).isEqualTo("{\"key\":\"value\"}");
	}

	@Test
	void sendShouldIncludeHeaders() throws Throwable {
		this.mockWebServer.enqueue(new MockResponse().setResponseCode(200));
		String url = this.mockWebServer.url("/test").toString();
		this.sender.post(url).withHeader("X-Custom", "custom-value").send();
		RecordedRequest request = this.mockWebServer.takeRequest();
		assertThat(request.getHeader("X-Custom")).isEqualTo("custom-value");
	}

	@Test
	void sendShouldHandleServerError() throws Throwable {
		this.mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("error"));
		String url = this.mockWebServer.url("/test").toString();
		Response response = this.sender.get(url).send();
		assertThat(response.code()).isEqualTo(500);
		assertThat(response.body()).isEqualTo("error");
	}

	@Test
	void sendShouldTimeoutOnSlowResponse() {
		JdkClientHttpSender sender = new JdkClientHttpSender(Duration.ofSeconds(5), Duration.ofMillis(10), null);
		this.mockWebServer.enqueue(new MockResponse().setResponseCode(200).setHeadersDelay(500, TimeUnit.MILLISECONDS));
		String url = this.mockWebServer.url("/test").toString();
		assertThatIOException().isThrownBy(() -> sender.get(url).send()).withMessageContaining("timed out");
	}

	@Test
	void sendShouldSendPutRequest() throws Throwable {
		this.mockWebServer.enqueue(new MockResponse().setResponseCode(200));
		String url = this.mockWebServer.url("/test").toString();
		this.sender.put(url).withPlainText("data").send();
		RecordedRequest request = this.mockWebServer.takeRequest();
		assertThat(request.getMethod()).isEqualTo("PUT");
		assertThat(request.getBody().readUtf8()).isEqualTo("data");
	}

	@Test
	void sendShouldSendDeleteRequest() throws Throwable {
		this.mockWebServer.enqueue(new MockResponse().setResponseCode(204));
		String url = this.mockWebServer.url("/test").toString();
		Response response = this.sender.delete(url).send();
		assertThat(response.code()).isEqualTo(204);
		RecordedRequest request = this.mockWebServer.takeRequest();
		assertThat(request.getMethod()).isEqualTo("DELETE");
	}

}
