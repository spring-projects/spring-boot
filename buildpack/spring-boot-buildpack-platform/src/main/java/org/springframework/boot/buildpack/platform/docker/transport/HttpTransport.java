/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.docker.transport;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import org.apache.hc.core5.http.Header;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.buildpack.platform.docker.configuration.DockerConnectionConfiguration;
import org.springframework.boot.buildpack.platform.docker.configuration.DockerHost;
import org.springframework.boot.buildpack.platform.docker.configuration.ResolvedDockerHost;
import org.springframework.boot.buildpack.platform.io.IOConsumer;

/**
 * HTTP transport used for docker access.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 2.3.0
 */
public interface HttpTransport {

	/**
	 * Perform an HTTP GET operation.
	 * @param uri the destination URI (excluding any host/port)
	 * @return the operation response
	 * @throws IOException on IO error
	 */
	Response get(URI uri) throws IOException;

	/**
	 * Perform an HTTP POST operation.
	 * @param uri the destination URI (excluding any host/port)
	 * @return the operation response
	 * @throws IOException on IO error
	 */
	Response post(URI uri) throws IOException;

	/**
	 * Perform an HTTP POST operation.
	 * @param uri the destination URI (excluding any host/port)
	 * @param registryAuth registry authentication credentials
	 * @return the operation response
	 * @throws IOException on IO error
	 */
	Response post(URI uri, @Nullable String registryAuth) throws IOException;

	/**
	 * Perform an HTTP POST operation.
	 * @param uri the destination URI (excluding any host/port)
	 * @param contentType the content type to write
	 * @param writer a content writer
	 * @return the operation response
	 * @throws IOException on IO error
	 */
	Response post(URI uri, String contentType, IOConsumer<OutputStream> writer) throws IOException;

	/**
	 * Perform an HTTP PUT operation.
	 * @param uri the destination URI (excluding any host/port)
	 * @param contentType the content type to write
	 * @param writer a content writer
	 * @return the operation response
	 * @throws IOException on IO error
	 */
	Response put(URI uri, String contentType, IOConsumer<OutputStream> writer) throws IOException;

	/**
	 * Perform an HTTP DELETE operation.
	 * @param uri the destination URI (excluding any host/port)
	 * @return the operation response
	 * @throws IOException on IO error
	 */
	Response delete(URI uri) throws IOException;

	/**
	 * Perform an HTTP HEAD operation.
	 * @param uri the destination URI (excluding any host/port)
	 * @return the operation response
	 * @throws IOException on IO error
	 */
	Response head(URI uri) throws IOException;

	/**
	 * Create the most suitable {@link HttpTransport} based on the {@link DockerHost}.
	 * @param connectionConfiguration the Docker host information
	 * @return a {@link HttpTransport} instance
	 */
	static HttpTransport create(@Nullable DockerConnectionConfiguration connectionConfiguration) {
		ResolvedDockerHost host = ResolvedDockerHost.from(connectionConfiguration);
		HttpTransport remote = RemoteHttpClientTransport.createIfPossible(host);
		return (remote != null) ? remote : LocalHttpClientTransport.create(host);
	}

	/**
	 * An HTTP operation response.
	 */
	interface Response extends Closeable {

		/**
		 * Return the content of the response.
		 * @return the response content
		 * @throws IOException on IO error
		 */
		InputStream getContent() throws IOException;

		default @Nullable Header getHeader(String name) {
			throw new UnsupportedOperationException();
		}

	}

}
