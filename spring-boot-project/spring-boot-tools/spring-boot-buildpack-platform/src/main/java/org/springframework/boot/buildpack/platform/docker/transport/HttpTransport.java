/*
 * Copyright 2012-2025 the original author or authors.
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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import org.apache.hc.core5.http.Header;

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
	Response post(URI uri, String registryAuth) throws IOException;

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
	 * @param dockerHost the Docker host information
	 * @return a {@link HttpTransport} instance
	 * @deprecated since 3.5.0 for removal in 4.0.0 in favor of
	 * {@link #create(DockerConnectionConfiguration)}
	 */
	@Deprecated(since = "3.5.0", forRemoval = true)
	@SuppressWarnings("removal")
	static HttpTransport create(
			org.springframework.boot.buildpack.platform.docker.configuration.DockerConfiguration.DockerHostConfiguration dockerHost) {
		ResolvedDockerHost host = ResolvedDockerHost.from(dockerHost);
		HttpTransport remote = RemoteHttpClientTransport.createIfPossible(host);
		return (remote != null) ? remote : LocalHttpClientTransport.create(host);
	}

	/**
	 * Create the most suitable {@link HttpTransport} based on the {@link DockerHost}.
	 * @param connectionConfiguration the Docker host information
	 * @return a {@link HttpTransport} instance
	 */
	static HttpTransport create(DockerConnectionConfiguration connectionConfiguration) {
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

		default Header getHeader(String name) {
			throw new UnsupportedOperationException();
		}

	}

}
