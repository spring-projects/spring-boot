/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.buildpack.platform.docker.transport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.buildpack.platform.docker.transport.HttpTransport.Response;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link HttpClientTransport}.
 *
 * @author Phillip Webb
 * @author Mike Smithson
 * @author Scott Frederick
 */
@ExtendWith(MockitoExtension.class)
class HttpClientTransportTests {

	private static final String APPLICATION_JSON = "application/json";

	private static final String APPLICATION_X_TAR = "application/x-tar";

	@Mock
	private CloseableHttpClient client;

	@Mock
	private CloseableHttpResponse response;

	@Mock
	private StatusLine statusLine;

	@Mock
	private HttpEntity entity;

	@Mock
	private InputStream content;

	@Captor
	private ArgumentCaptor<HttpHost> hostCaptor;

	@Captor
	private ArgumentCaptor<HttpUriRequest> requestCaptor;

	private HttpClientTransport http;

	private URI uri;

	@BeforeEach
	void setup() throws Exception {
		this.http = new TestHttpClientTransport(this.client);
		this.uri = new URI("example");
	}

	@Test
	void getShouldExecuteHttpGet() throws Exception {
		givenClientWillReturnResponse();
		given(this.entity.getContent()).willReturn(this.content);
		given(this.statusLine.getStatusCode()).willReturn(200);
		Response response = this.http.get(this.uri);
		verify(this.client).execute(this.hostCaptor.capture(), this.requestCaptor.capture());
		HttpUriRequest request = this.requestCaptor.getValue();
		assertThat(request).isInstanceOf(HttpGet.class);
		assertThat(request.getURI()).isEqualTo(this.uri);
		assertThat(request.getFirstHeader(HttpHeaders.CONTENT_TYPE)).isNull();
		assertThat(response.getContent()).isSameAs(this.content);
	}

	@Test
	void postShouldExecuteHttpPost() throws Exception {
		givenClientWillReturnResponse();
		given(this.entity.getContent()).willReturn(this.content);
		given(this.statusLine.getStatusCode()).willReturn(200);
		Response response = this.http.post(this.uri);
		verify(this.client).execute(this.hostCaptor.capture(), this.requestCaptor.capture());
		HttpUriRequest request = this.requestCaptor.getValue();
		assertThat(request).isInstanceOf(HttpPost.class);
		assertThat(request.getURI()).isEqualTo(this.uri);
		assertThat(request.getFirstHeader(HttpHeaders.CONTENT_TYPE)).isNull();
		assertThat(request.getFirstHeader(HttpClientTransport.REGISTRY_AUTH_HEADER)).isNull();
		assertThat(response.getContent()).isSameAs(this.content);
	}

	@Test
	void postWithRegistryAuthShouldExecuteHttpPostWithHeader() throws Exception {
		givenClientWillReturnResponse();
		given(this.entity.getContent()).willReturn(this.content);
		given(this.statusLine.getStatusCode()).willReturn(200);
		Response response = this.http.post(this.uri, "auth token");
		verify(this.client).execute(this.hostCaptor.capture(), this.requestCaptor.capture());
		HttpUriRequest request = this.requestCaptor.getValue();
		assertThat(request).isInstanceOf(HttpPost.class);
		assertThat(request.getURI()).isEqualTo(this.uri);
		assertThat(request.getFirstHeader(HttpHeaders.CONTENT_TYPE)).isNull();
		assertThat(request.getFirstHeader(HttpClientTransport.REGISTRY_AUTH_HEADER).getValue()).isEqualTo("auth token");
		assertThat(response.getContent()).isSameAs(this.content);
	}

	@Test
	void postWithEmptyRegistryAuthShouldExecuteHttpPostWithoutHeader() throws Exception {
		givenClientWillReturnResponse();
		given(this.entity.getContent()).willReturn(this.content);
		given(this.statusLine.getStatusCode()).willReturn(200);
		Response response = this.http.post(this.uri, "");
		verify(this.client).execute(this.hostCaptor.capture(), this.requestCaptor.capture());
		HttpUriRequest request = this.requestCaptor.getValue();
		assertThat(request).isInstanceOf(HttpPost.class);
		assertThat(request.getURI()).isEqualTo(this.uri);
		assertThat(request.getFirstHeader(HttpHeaders.CONTENT_TYPE)).isNull();
		assertThat(request.getFirstHeader(HttpClientTransport.REGISTRY_AUTH_HEADER)).isNull();
		assertThat(response.getContent()).isSameAs(this.content);
	}

	@Test
	void postWithJsonContentShouldExecuteHttpPost() throws Exception {
		String content = "test";
		givenClientWillReturnResponse();
		given(this.entity.getContent()).willReturn(this.content);
		given(this.statusLine.getStatusCode()).willReturn(200);
		Response response = this.http.post(this.uri, APPLICATION_JSON,
				(out) -> StreamUtils.copy(content, StandardCharsets.UTF_8, out));
		verify(this.client).execute(this.hostCaptor.capture(), this.requestCaptor.capture());
		HttpUriRequest request = this.requestCaptor.getValue();
		HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
		assertThat(request).isInstanceOf(HttpPost.class);
		assertThat(request.getURI()).isEqualTo(this.uri);
		assertThat(entity.isRepeatable()).isFalse();
		assertThat(entity.getContentLength()).isEqualTo(content.length());
		assertThat(entity.getContentType().getValue()).isEqualTo(APPLICATION_JSON);
		assertThat(entity.isStreaming()).isTrue();
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(entity::getContent);
		assertThat(writeToString(entity)).isEqualTo(content);
		assertThat(response.getContent()).isSameAs(this.content);
	}

	@Test
	void postWithArchiveContentShouldExecuteHttpPost() throws Exception {
		String content = "test";
		givenClientWillReturnResponse();
		given(this.entity.getContent()).willReturn(this.content);
		given(this.statusLine.getStatusCode()).willReturn(200);
		Response response = this.http.post(this.uri, APPLICATION_X_TAR,
				(out) -> StreamUtils.copy(content, StandardCharsets.UTF_8, out));
		verify(this.client).execute(this.hostCaptor.capture(), this.requestCaptor.capture());
		HttpUriRequest request = this.requestCaptor.getValue();
		HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
		assertThat(request).isInstanceOf(HttpPost.class);
		assertThat(request.getURI()).isEqualTo(this.uri);
		assertThat(entity.isRepeatable()).isFalse();
		assertThat(entity.getContentLength()).isEqualTo(-1);
		assertThat(entity.getContentType().getValue()).isEqualTo(APPLICATION_X_TAR);
		assertThat(entity.isStreaming()).isTrue();
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(entity::getContent);
		assertThat(writeToString(entity)).isEqualTo(content);
		assertThat(response.getContent()).isSameAs(this.content);
	}

	@Test
	void putWithJsonContentShouldExecuteHttpPut() throws Exception {
		String content = "test";
		givenClientWillReturnResponse();
		given(this.entity.getContent()).willReturn(this.content);
		given(this.statusLine.getStatusCode()).willReturn(200);
		Response response = this.http.put(this.uri, APPLICATION_JSON,
				(out) -> StreamUtils.copy(content, StandardCharsets.UTF_8, out));
		verify(this.client).execute(this.hostCaptor.capture(), this.requestCaptor.capture());
		HttpUriRequest request = this.requestCaptor.getValue();
		HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
		assertThat(request).isInstanceOf(HttpPut.class);
		assertThat(request.getURI()).isEqualTo(this.uri);
		assertThat(entity.isRepeatable()).isFalse();
		assertThat(entity.getContentLength()).isEqualTo(content.length());
		assertThat(entity.getContentType().getValue()).isEqualTo(APPLICATION_JSON);
		assertThat(entity.isStreaming()).isTrue();
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(entity::getContent);
		assertThat(writeToString(entity)).isEqualTo(content);
		assertThat(response.getContent()).isSameAs(this.content);
	}

	@Test
	void putWithArchiveContentShouldExecuteHttpPut() throws Exception {
		String content = "test";
		givenClientWillReturnResponse();
		given(this.entity.getContent()).willReturn(this.content);
		given(this.statusLine.getStatusCode()).willReturn(200);
		Response response = this.http.put(this.uri, APPLICATION_X_TAR,
				(out) -> StreamUtils.copy(content, StandardCharsets.UTF_8, out));
		verify(this.client).execute(this.hostCaptor.capture(), this.requestCaptor.capture());
		HttpUriRequest request = this.requestCaptor.getValue();
		HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
		assertThat(request).isInstanceOf(HttpPut.class);
		assertThat(request.getURI()).isEqualTo(this.uri);
		assertThat(entity.isRepeatable()).isFalse();
		assertThat(entity.getContentLength()).isEqualTo(-1);
		assertThat(entity.getContentType().getValue()).isEqualTo(APPLICATION_X_TAR);
		assertThat(entity.isStreaming()).isTrue();
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(entity::getContent);
		assertThat(writeToString(entity)).isEqualTo(content);
		assertThat(response.getContent()).isSameAs(this.content);
	}

	@Test
	void deleteShouldExecuteHttpDelete() throws IOException {
		givenClientWillReturnResponse();
		given(this.entity.getContent()).willReturn(this.content);
		given(this.statusLine.getStatusCode()).willReturn(200);
		Response response = this.http.delete(this.uri);
		verify(this.client).execute(this.hostCaptor.capture(), this.requestCaptor.capture());
		HttpUriRequest request = this.requestCaptor.getValue();
		assertThat(request).isInstanceOf(HttpDelete.class);
		assertThat(request.getURI()).isEqualTo(this.uri);
		assertThat(request.getFirstHeader(HttpHeaders.CONTENT_TYPE)).isNull();
		assertThat(response.getContent()).isSameAs(this.content);
	}

	@Test
	void executeWhenResponseIsIn400RangeShouldThrowDockerException() throws IOException {
		givenClientWillReturnResponse();
		given(this.entity.getContent()).willReturn(getClass().getResourceAsStream("errors.json"));
		given(this.statusLine.getStatusCode()).willReturn(404);
		assertThatExceptionOfType(DockerEngineException.class).isThrownBy(() -> this.http.get(this.uri))
				.satisfies((ex) -> {
					assertThat(ex.getErrors()).hasSize(2);
					assertThat(ex.getResponseMessage()).isNull();
				});
	}

	@Test
	void executeWhenResponseIsIn500RangeWithNoContentShouldThrowDockerException() throws IOException {
		givenClientWillReturnResponse();
		given(this.statusLine.getStatusCode()).willReturn(500);
		assertThatExceptionOfType(DockerEngineException.class).isThrownBy(() -> this.http.get(this.uri))
				.satisfies((ex) -> {
					assertThat(ex.getErrors()).isNull();
					assertThat(ex.getResponseMessage()).isNull();
				});
	}

	@Test
	void executeWhenResponseIsIn500RangeWithMessageShouldThrowDockerException() throws IOException {
		givenClientWillReturnResponse();
		given(this.entity.getContent()).willReturn(getClass().getResourceAsStream("message.json"));
		given(this.statusLine.getStatusCode()).willReturn(500);
		assertThatExceptionOfType(DockerEngineException.class).isThrownBy(() -> this.http.get(this.uri))
				.satisfies((ex) -> {
					assertThat(ex.getErrors()).isNull();
					assertThat(ex.getResponseMessage().getMessage()).contains("test message");
				});
	}

	@Test
	void executeWhenResponseIsIn500RangeWithOtherContentShouldThrowDockerException() throws IOException {
		givenClientWillReturnResponse();
		given(this.entity.getContent()).willReturn(this.content);
		given(this.statusLine.getStatusCode()).willReturn(500);
		assertThatExceptionOfType(DockerEngineException.class).isThrownBy(() -> this.http.get(this.uri))
				.satisfies((ex) -> {
					assertThat(ex.getErrors()).isNull();
					assertThat(ex.getResponseMessage()).isNull();
				});
	}

	@Test
	void executeWhenClientThrowsIOExceptionRethrowsAsDockerException() throws IOException {
		given(this.client.execute(any(HttpHost.class), any(HttpRequest.class)))
				.willThrow(new IOException("test IO exception"));
		assertThatExceptionOfType(DockerConnectionException.class).isThrownBy(() -> this.http.get(this.uri))
				.satisfies((ex) -> assertThat(ex.getMessage()).contains("test IO exception"));
	}

	private String writeToString(HttpEntity entity) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		entity.writeTo(out);
		return new String(out.toByteArray(), StandardCharsets.UTF_8);
	}

	private void givenClientWillReturnResponse() throws IOException {
		given(this.client.execute(any(HttpHost.class), any(HttpRequest.class))).willReturn(this.response);
		given(this.response.getEntity()).willReturn(this.entity);
		given(this.response.getStatusLine()).willReturn(this.statusLine);
	}

	/**
	 * Test {@link HttpClientTransport} implementation.
	 */
	static class TestHttpClientTransport extends HttpClientTransport {

		protected TestHttpClientTransport(CloseableHttpClient client) {
			super(client, HttpHost.create("docker://localhost"));
		}

	}

}
