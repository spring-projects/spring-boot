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

package org.springframework.boot.docker.compose.core;

import java.net.URI;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.util.StringUtils;

/**
 * A docker host as defined by the user or deduced.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
final class DockerHost {

	private static final String LOCALHOST = "127.0.0.1";

	private final String host;

	/**
	 * Constructs a new DockerHost object with the specified host.
	 * @param host the host address of the Docker host
	 */
	private DockerHost(String host) {
		this.host = host;
	}

	/**
	 * Compares this DockerHost object to the specified object for equality.
	 * @param obj the object to compare to
	 * @return true if the specified object is equal to this DockerHost object, false
	 * otherwise
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		DockerHost other = (DockerHost) obj;
		return this.host.equals(other.host);
	}

	/**
	 * Returns the hash code value for the DockerHost object.
	 * @return the hash code value for the DockerHost object
	 */
	@Override
	public int hashCode() {
		return this.host.hashCode();
	}

	/**
	 * Returns a string representation of the DockerHost object.
	 * @return the host name of the DockerHost object
	 */
	@Override
	public String toString() {
		return this.host;
	}

	/**
	 * Get or deduce a new {@link DockerHost} instance.
	 * @param host the host to use or {@code null} to deduce
	 * @param contextsSupplier a supplier to provide a list of
	 * {@link DockerCliContextResponse}
	 * @return a new docker host instance
	 */
	static DockerHost get(String host, Supplier<List<DockerCliContextResponse>> contextsSupplier) {
		return get(host, System::getenv, contextsSupplier);
	}

	/**
	 * Get or deduce a new {@link DockerHost} instance.
	 * @param host the host to use or {@code null} to deduce
	 * @param systemEnv access to the system environment
	 * @param contextsSupplier a supplier to provide a list of
	 * {@link DockerCliContextResponse}
	 * @return a new docker host instance
	 */
	static DockerHost get(String host, Function<String, String> systemEnv,
			Supplier<List<DockerCliContextResponse>> contextsSupplier) {
		host = (StringUtils.hasText(host)) ? host : fromServicesHostEnv(systemEnv);
		host = (StringUtils.hasText(host)) ? host : fromDockerHostEnv(systemEnv);
		host = (StringUtils.hasText(host)) ? host : fromCurrentContext(contextsSupplier);
		host = (StringUtils.hasText(host)) ? host : LOCALHOST;
		return new DockerHost(host);
	}

	/**
	 * Retrieves the value of the SERVICES_HOST environment variable.
	 * @param systemEnv a function that retrieves environment variable values
	 * @return the value of the SERVICES_HOST environment variable
	 */
	private static String fromServicesHostEnv(Function<String, String> systemEnv) {
		return systemEnv.apply("SERVICES_HOST");
	}

	/**
	 * Converts the Docker host environment variable to a formatted endpoint string.
	 * @param systemEnv the function to retrieve system environment variables
	 * @return the formatted endpoint string
	 */
	private static String fromDockerHostEnv(Function<String, String> systemEnv) {
		return fromEndpoint(systemEnv.apply("DOCKER_HOST"));
	}

	/**
	 * Retrieves the Docker host endpoint from the current context.
	 * @param contextsSupplier a supplier function that provides a list of Docker CLI
	 * contexts
	 * @return the Docker host endpoint if found in the current context, null otherwise
	 */
	private static String fromCurrentContext(Supplier<List<DockerCliContextResponse>> contextsSupplier) {
		DockerCliContextResponse current = getCurrentContext(contextsSupplier.get());
		return (current != null) ? fromEndpoint(current.dockerEndpoint()) : null;
	}

	/**
	 * Returns the current Docker CLI context from the given list of candidates.
	 * @param candidates the list of Docker CLI context responses
	 * @return the current Docker CLI context, or null if not found
	 */
	private static DockerCliContextResponse getCurrentContext(List<DockerCliContextResponse> candidates) {
		return candidates.stream().filter(DockerCliContextResponse::current).findFirst().orElse(null);
	}

	/**
	 * Converts the given endpoint string to a URI string representation.
	 * @param endpoint the endpoint string to convert
	 * @return the URI string representation of the endpoint, or null if the endpoint is
	 * empty or null
	 */
	private static String fromEndpoint(String endpoint) {
		return (StringUtils.hasLength(endpoint)) ? fromUri(URI.create(endpoint)) : null;
	}

	/**
	 * Returns the host name from the given URI.
	 * @param uri the URI from which to extract the host name
	 * @return the host name if the URI scheme is "http", "https", or "tcp", otherwise
	 * null
	 */
	private static String fromUri(URI uri) {
		try {
			return switch (uri.getScheme()) {
				case "http", "https", "tcp" -> uri.getHost();
				default -> null;
			};
		}
		catch (Exception ex) {
			return null;
		}
	}

}
