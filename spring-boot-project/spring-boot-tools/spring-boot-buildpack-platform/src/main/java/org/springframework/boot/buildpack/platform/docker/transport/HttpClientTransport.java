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

package org.springframework.boot.buildpack.platform.docker.transport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.entity.AbstractHttpEntity;
import org.apache.hc.core5.http.message.StatusLine;

import org.springframework.boot.buildpack.platform.io.Content;
import org.springframework.boot.buildpack.platform.io.IOConsumer;
import org.springframework.boot.buildpack.platform.json.SharedObjectMapper;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Abstract base class for {@link HttpTransport} implementations backed by a
 * {@link HttpClient}.
 *
 * @author Phillip Webb
 * @author Mike Smithson
 * @author Scott Frederick
 */
abstract class HttpClientTransport implements HttpTransport {

	static final String REGISTRY_AUTH_HEADER = "X-Registry-Auth";

	private final HttpClient client;

	private final HttpHost host;

	/**
     * Constructs a new instance of the HttpClientTransport class.
     * 
     * @param client the HttpClient object to be used for the transport
     * @param host the HttpHost object representing the target host
     * @throws IllegalArgumentException if either the client or host parameter is null
     */
    protected HttpClientTransport(HttpClient client, HttpHost host) {
		Assert.notNull(client, "Client must not be null");
		Assert.notNull(host, "Host must not be null");
		this.client = client;
		this.host = host;
	}

	/**
	 * Perform an HTTP GET operation.
	 * @param uri the destination URI
	 * @return the operation response
	 */
	@Override
	public Response get(URI uri) {
		return execute(new HttpGet(uri));
	}

	/**
	 * Perform an HTTP POST operation.
	 * @param uri the destination URI
	 * @return the operation response
	 */
	@Override
	public Response post(URI uri) {
		return execute(new HttpPost(uri));
	}

	/**
	 * Perform an HTTP POST operation.
	 * @param uri the destination URI
	 * @param registryAuth registry authentication credentials
	 * @return the operation response
	 */
	@Override
	public Response post(URI uri, String registryAuth) {
		return execute(new HttpPost(uri), registryAuth);
	}

	/**
	 * Perform an HTTP POST operation.
	 * @param uri the destination URI
	 * @param contentType the content type to write
	 * @param writer a content writer
	 * @return the operation response
	 */
	@Override
	public Response post(URI uri, String contentType, IOConsumer<OutputStream> writer) {
		return execute(new HttpPost(uri), contentType, writer);
	}

	/**
	 * Perform an HTTP PUT operation.
	 * @param uri the destination URI
	 * @param contentType the content type to write
	 * @param writer a content writer
	 * @return the operation response
	 */
	@Override
	public Response put(URI uri, String contentType, IOConsumer<OutputStream> writer) {
		return execute(new HttpPut(uri), contentType, writer);
	}

	/**
	 * Perform an HTTP DELETE operation.
	 * @param uri the destination URI
	 * @return the operation response
	 */
	@Override
	public Response delete(URI uri) {
		return execute(new HttpDelete(uri));
	}

	/**
     * Executes an HTTP request with the given request object, content type, and writer.
     * 
     * @param request the HTTP request object to be executed
     * @param contentType the content type of the request body
     * @param writer the consumer function that writes the request body to the output stream
     * @return the HTTP response received from the server
     */
    private Response execute(HttpUriRequestBase request, String contentType, IOConsumer<OutputStream> writer) {
		request.setEntity(new WritableHttpEntity(contentType, writer));
		return execute(request);
	}

	/**
     * Executes an HTTP request with an optional registry authentication header.
     * 
     * @param request the HTTP request to be executed
     * @param registryAuth the registry authentication header value
     * @return the HTTP response
     */
    private Response execute(HttpUriRequestBase request, String registryAuth) {
		if (StringUtils.hasText(registryAuth)) {
			request.setHeader(REGISTRY_AUTH_HEADER, registryAuth);
		}
		return execute(request);
	}

	/**
     * Executes an HTTP request and returns the response.
     * 
     * @param request the HTTP request to execute
     * @return the HTTP response
     * @throws DockerEngineException if the response status code is between 400 and 500 (inclusive)
     * @throws DockerConnectionException if an I/O error or URI syntax error occurs
     */
    private Response execute(HttpUriRequest request) {
		try {
			ClassicHttpResponse response = this.client.executeOpen(this.host, request, null);
			int statusCode = response.getCode();
			if (statusCode >= 400 && statusCode <= 500) {
				HttpEntity entity = response.getEntity();
				Errors errors = (statusCode != 500) ? getErrorsFromResponse(entity) : null;
				Message message = getMessageFromResponse(entity);
				StatusLine statusLine = new StatusLine(response);
				throw new DockerEngineException(this.host.toHostString(), request.getUri(), statusCode,
						statusLine.getReasonPhrase(), errors, message);
			}
			return new HttpClientResponse(response);
		}
		catch (IOException | URISyntaxException ex) {
			throw new DockerConnectionException(this.host.toHostString(), ex);
		}
	}

	/**
     * Retrieves the Errors object from the given HttpEntity.
     * 
     * @param entity the HttpEntity from which to retrieve the Errors object
     * @return the Errors object parsed from the HttpEntity, or null if an IOException occurs
     */
    private Errors getErrorsFromResponse(HttpEntity entity) {
		try {
			return SharedObjectMapper.get().readValue(entity.getContent(), Errors.class);
		}
		catch (IOException ex) {
			return null;
		}
	}

	/**
     * Retrieves a Message object from the given HttpEntity.
     * 
     * @param entity the HttpEntity from which to retrieve the Message object
     * @return the Message object retrieved from the HttpEntity, or null if the HttpEntity is null or an error occurs
     */
    private Message getMessageFromResponse(HttpEntity entity) {
		try {
			return (entity.getContent() != null)
					? SharedObjectMapper.get().readValue(entity.getContent(), Message.class) : null;
		}
		catch (IOException ex) {
			return null;
		}
	}

	/**
     * Returns the HttpHost object representing the host of this HttpClientTransport.
     *
     * @return the HttpHost object representing the host of this HttpClientTransport
     */
    HttpHost getHost() {
		return this.host;
	}

	/**
	 * {@link HttpEntity} to send {@link Content} content.
	 */
	private static class WritableHttpEntity extends AbstractHttpEntity {

		private final IOConsumer<OutputStream> writer;

		/**
         * Constructs a new WritableHttpEntity with the specified content type and writer.
         * 
         * @param contentType the content type of the entity
         * @param writer the consumer function that writes the entity content to an output stream
         */
        WritableHttpEntity(String contentType, IOConsumer<OutputStream> writer) {
			super(contentType, "UTF-8");
			this.writer = writer;
		}

		/**
         * Returns whether this entity is repeatable.
         * 
         * @return {@code true} if this entity is repeatable, {@code false} otherwise.
         */
        @Override
		public boolean isRepeatable() {
			return false;
		}

		/**
         * Returns the content length of the HTTP entity.
         * 
         * @return the content length, or -1 if the content type is not "application/json"
         */
        @Override
		public long getContentLength() {
			if (this.getContentType() != null && this.getContentType().equals("application/json")) {
				return calculateStringContentLength();
			}
			return -1;
		}

		/**
         * Returns the content of this WritableHttpEntity as an InputStream.
         *
         * @return the content of this WritableHttpEntity as an InputStream
         * @throws UnsupportedOperationException if the operation is not supported
         */
        @Override
		public InputStream getContent() throws UnsupportedOperationException {
			throw new UnsupportedOperationException();
		}

		/**
         * Writes the content of this entity to the specified output stream.
         *
         * @param outputStream the output stream to write the content to
         * @throws IOException if an I/O error occurs while writing the content
         */
        @Override
		public void writeTo(OutputStream outputStream) throws IOException {
			this.writer.accept(outputStream);
		}

		/**
         * Returns whether the entity is streaming.
         * 
         * @return true if the entity is streaming, false otherwise
         */
        @Override
		public boolean isStreaming() {
			return true;
		}

		/**
         * Calculates the length of the content in the string representation of the entity.
         * 
         * @return The length of the content in bytes, or -1 if an IOException occurs.
         */
        private int calculateStringContentLength() {
			try {
				ByteArrayOutputStream bytes = new ByteArrayOutputStream();
				this.writer.accept(bytes);
				return bytes.toByteArray().length;
			}
			catch (IOException ex) {
				return -1;
			}
		}

		/**
         * Closes the WritableHttpEntity.
         *
         * @throws IOException if an I/O error occurs while closing the entity
         */
        @Override
		public void close() throws IOException {
		}

	}

	/**
	 * An HTTP operation response.
	 */
	private static class HttpClientResponse implements Response {

		private final ClassicHttpResponse response;

		/**
         * Constructs a new HttpClientResponse object with the given ClassicHttpResponse.
         * 
         * @param response the ClassicHttpResponse object representing the response received from the server
         */
        HttpClientResponse(ClassicHttpResponse response) {
			this.response = response;
		}

		/**
         * Returns the content of the response as an InputStream.
         * 
         * @return the content of the response as an InputStream
         * @throws IOException if an I/O error occurs while getting the content
         */
        @Override
		public InputStream getContent() throws IOException {
			return this.response.getEntity().getContent();
		}

		/**
         * Closes the HttpClientResponse and releases any system resources associated with it.
         * 
         * @throws IOException if an I/O error occurs while closing the response
         */
        @Override
		public void close() throws IOException {
			this.response.close();
		}

	}

}
