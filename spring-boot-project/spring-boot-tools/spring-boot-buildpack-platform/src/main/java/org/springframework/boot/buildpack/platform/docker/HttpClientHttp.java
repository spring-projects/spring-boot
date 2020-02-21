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

package org.springframework.boot.buildpack.platform.docker;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;

import org.springframework.boot.buildpack.platform.io.Content;
import org.springframework.boot.buildpack.platform.io.IOConsumer;
import org.springframework.boot.buildpack.platform.json.SharedObjectMapper;

/**
 * {@link Http} implementation backed by a {@link HttpClient}.
 *
 * @author Phillip Webb
 */
class HttpClientHttp implements Http {

	private final CloseableHttpClient client;

	HttpClientHttp() {
		HttpClientBuilder builder = HttpClients.custom();
		builder.setConnectionManager(new DockerHttpClientConnectionManager());
		builder.setSchemePortResolver(new DockerSchemePortResolver());
		this.client = builder.build();
	}

	HttpClientHttp(CloseableHttpClient client) {
		this.client = client;
	}

	/**
	 * Perform a HTTP GET operation.
	 * @param uri the destination URI
	 * @return the operation response
	 * @throws IOException on IO error
	 */
	@Override
	public Response get(URI uri) throws IOException {
		return execute(new HttpGet(uri));
	}

	/**
	 * Perform a HTTP POST operation.
	 * @param uri the destination URI
	 * @return the operation response
	 * @throws IOException on IO error
	 */
	@Override
	public Response post(URI uri) throws IOException {
		return execute(new HttpPost(uri));
	}

	/**
	 * Perform a HTTP POST operation.
	 * @param uri the destination URI
	 * @param contentType the content type to write
	 * @param writer a content writer
	 * @return the operation response
	 * @throws IOException on IO error
	 */

	@Override
	public Response post(URI uri, String contentType, IOConsumer<OutputStream> writer) throws IOException {
		return execute(new HttpPost(uri), contentType, writer);
	}

	/**
	 * Perform a HTTP PUT operation.
	 * @param uri the destination URI
	 * @param contentType the content type to write
	 * @param writer a content writer
	 * @return the operation response
	 * @throws IOException on IO error
	 */

	@Override
	public Response put(URI uri, String contentType, IOConsumer<OutputStream> writer) throws IOException {
		return execute(new HttpPut(uri), contentType, writer);
	}

	/**
	 * Perform a HTTP DELETE operation.
	 * @param uri the destination URI
	 * @return the operation response
	 * @throws IOException on IO error
	 */

	@Override
	public Response delete(URI uri) throws IOException {
		return execute(new HttpDelete(uri));
	}

	private Response execute(HttpEntityEnclosingRequestBase request, String contentType,
			IOConsumer<OutputStream> writer) throws IOException {
		request.setHeader(HttpHeaders.CONTENT_TYPE, contentType);
		request.setEntity(new WritableHttpEntity(writer));
		return execute(request);
	}

	private Response execute(HttpUriRequest request) throws IOException {
		CloseableHttpResponse response = this.client.execute(request);
		StatusLine statusLine = response.getStatusLine();
		int statusCode = statusLine.getStatusCode();
		HttpEntity entity = response.getEntity();

		if (statusCode >= 400 && statusCode < 500) {
			Errors errors = SharedObjectMapper.get().readValue(entity.getContent(), Errors.class);
			throw new DockerException(request.getURI(), statusCode, statusLine.getReasonPhrase(), errors);
		}
		if (statusCode == 500) {
			throw new DockerException(request.getURI(), statusCode, statusLine.getReasonPhrase(), null);
		}

		return new HttpClientResponse(response);
	}

	/**
	 * {@link HttpEntity} to send {@link Content} content.
	 *
	 * @author Phillip Webb
	 */
	private static class WritableHttpEntity extends AbstractHttpEntity {

		private final IOConsumer<OutputStream> writer;

		WritableHttpEntity(IOConsumer<OutputStream> writer) {
			this.writer = writer;
		}

		@Override
		public boolean isRepeatable() {
			return false;
		}

		@Override
		public long getContentLength() {
			return -1;
		}

		@Override
		public InputStream getContent() throws UnsupportedOperationException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void writeTo(OutputStream outputStream) throws IOException {
			this.writer.accept(outputStream);
		}

		@Override
		public boolean isStreaming() {
			return true;
		}

	}

	/**
	 * An HTTP operation response.
	 */
	private static class HttpClientResponse implements Response {

		private final CloseableHttpResponse response;

		HttpClientResponse(CloseableHttpResponse response) {
			this.response = response;
		}

		@Override
		public InputStream getContent() throws IOException {
			return this.response.getEntity().getContent();
		}

		@Override
		public void close() throws IOException {
			this.response.close();
		}

	}

}
