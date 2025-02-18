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

package org.springframework.boot.cli.command.init;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.message.BasicHeader;
import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.ArgumentMatcher;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Abstract base class for tests that use a mock {@link HttpClient}.
 *
 * @author Stephane Nicoll
 */
public abstract class AbstractHttpClientMockTests {

	protected final HttpClient http = mock(HttpClient.class);

	protected void mockSuccessfulMetadataTextGet() throws IOException {
		mockSuccessfulMetadataGet("metadata/service-metadata-2.1.0.txt", "text/plain", true);
	}

	protected void mockSuccessfulMetadataGet(boolean serviceCapabilities) throws IOException {
		mockSuccessfulMetadataGet("metadata/service-metadata-2.1.0.json", "application/vnd.initializr.v2.1+json",
				serviceCapabilities);
	}

	protected void mockSuccessfulMetadataGetV2(boolean serviceCapabilities) throws IOException {
		mockSuccessfulMetadataGet("metadata/service-metadata-2.0.0.json", "application/vnd.initializr.v2+json",
				serviceCapabilities);
	}

	protected void mockSuccessfulMetadataGet(String contentPath, String contentType, boolean serviceCapabilities)
			throws IOException {
		ClassicHttpResponse response = mock(ClassicHttpResponse.class);
		byte[] content = readClasspathResource(contentPath);
		mockHttpEntity(response, content, contentType);
		mockStatus(response, 200);
		given(this.http.executeOpen(any(HttpHost.class), argThat(getForMetadata(serviceCapabilities)), isNull()))
			.willReturn(response);
	}

	protected byte[] readClasspathResource(String contentPath) throws IOException {
		Resource resource = new ClassPathResource(contentPath);
		return StreamUtils.copyToByteArray(resource.getInputStream());
	}

	protected void mockSuccessfulProjectGeneration(MockHttpProjectGenerationRequest request) throws IOException {
		// Required for project generation as the metadata is read first
		mockSuccessfulMetadataGet(false);
		ClassicHttpResponse response = mock(ClassicHttpResponse.class);
		mockHttpEntity(response, request.content, request.contentType);
		mockStatus(response, 200);
		String header = (request.fileName != null) ? contentDispositionValue(request.fileName) : null;
		mockHttpHeader(response, "Content-Disposition", header);
		given(this.http.executeOpen(any(HttpHost.class), argThat(getForNonMetadata()), isNull())).willReturn(response);
	}

	protected void mockProjectGenerationError(int status, String message) throws IOException, JSONException {
		// Required for project generation as the metadata is read first
		mockSuccessfulMetadataGet(false);
		ClassicHttpResponse response = mock(ClassicHttpResponse.class);
		mockHttpEntity(response, createJsonError(status, message).getBytes(), "application/json");
		mockStatus(response, status);
		given(this.http.executeOpen(any(HttpHost.class), isA(HttpGet.class), isNull())).willReturn(response);
	}

	protected void mockMetadataGetError(int status, String message) throws IOException, JSONException {
		ClassicHttpResponse response = mock(ClassicHttpResponse.class);
		mockHttpEntity(response, createJsonError(status, message).getBytes(), "application/json");
		mockStatus(response, status);
		given(this.http.executeOpen(any(HttpHost.class), isA(HttpGet.class), isNull())).willReturn(response);
	}

	protected HttpEntity mockHttpEntity(ClassicHttpResponse response, byte[] content, String contentType) {
		try {
			HttpEntity entity = mock(HttpEntity.class);
			given(entity.getContent()).willReturn(new ByteArrayInputStream(content));
			Header contentTypeHeader = (contentType != null) ? new BasicHeader("Content-Type", contentType) : null;
			given(entity.getContentType())
				.willReturn((contentTypeHeader != null) ? contentTypeHeader.getValue() : null);
			given(response.getEntity()).willReturn(entity);
			return entity;
		}
		catch (IOException ex) {
			throw new IllegalStateException("Should not happen", ex);
		}
	}

	protected void mockStatus(ClassicHttpResponse response, int status) {
		given(response.getCode()).willReturn(status);
	}

	protected void mockHttpHeader(ClassicHttpResponse response, String headerName, String value) {
		Header header = (value != null) ? new BasicHeader(headerName, value) : null;
		given(response.getFirstHeader(headerName)).willReturn(header);
	}

	private ArgumentMatcher<HttpGet> getForMetadata(boolean serviceCapabilities) {
		if (!serviceCapabilities) {
			return new HasAcceptHeader(InitializrService.ACCEPT_META_DATA, true);
		}
		return new HasAcceptHeader(InitializrService.ACCEPT_SERVICE_CAPABILITIES, true);
	}

	private ArgumentMatcher<HttpGet> getForNonMetadata() {
		return new HasAcceptHeader(InitializrService.ACCEPT_META_DATA, false);
	}

	private String contentDispositionValue(String fileName) {
		return "attachment; filename=\"" + fileName + "\"";
	}

	private String createJsonError(int status, String message) throws JSONException {
		JSONObject json = new JSONObject();
		json.put("status", status);
		if (message != null) {
			json.put("message", message);
		}
		return json.toString();
	}

	static class MockHttpProjectGenerationRequest {

		String contentType;

		String fileName;

		byte[] content = new byte[] { 0, 0, 0, 0 };

		MockHttpProjectGenerationRequest(String contentType, String fileName) {
			this(contentType, fileName, new byte[] { 0, 0, 0, 0 });
		}

		MockHttpProjectGenerationRequest(String contentType, String fileName, byte[] content) {
			this.contentType = (contentType != null) ? contentType : "application/text";
			this.fileName = fileName;
			this.content = content;
		}

	}

	static class HasAcceptHeader implements ArgumentMatcher<HttpGet> {

		private final String value;

		private final boolean shouldMatch;

		HasAcceptHeader(String value, boolean shouldMatch) {
			this.value = value;
			this.shouldMatch = shouldMatch;
		}

		@Override
		public boolean matches(HttpGet get) {
			if (get == null) {
				return false;
			}
			Header acceptHeader = get.getFirstHeader(HttpHeaders.ACCEPT);
			if (this.shouldMatch) {
				return acceptHeader != null && this.value.equals(acceptHeader.getValue());
			}
			return acceptHeader == null || !this.value.equals(acceptHeader.getValue());
		}

	}

}
