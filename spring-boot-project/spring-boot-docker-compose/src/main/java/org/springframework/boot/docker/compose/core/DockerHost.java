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

	private DockerHost(String host) {
		this.host = host;
	}

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

	@Override
	public int hashCode() {
		return this.host.hashCode();
	}

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

	private static String fromServicesHostEnv(Function<String, String> systemEnv) {
		return systemEnv.apply("SERVICES_HOST");
	}

	private static String fromDockerHostEnv(Function<String, String> systemEnv) {
		return fromEndpoint(systemEnv.apply("DOCKER_HOST"));
	}

	private static String fromCurrentContext(Supplier<List<DockerCliContextResponse>> contextsSupplier) {
		DockerCliContextResponse current = getCurrentContext(contextsSupplier.get());
		return (current != null) ? fromEndpoint(current.dockerEndpoint()) : null;
	}

	private static DockerCliContextResponse getCurrentContext(List<DockerCliContextResponse> candidates) {
		return candidates.stream().filter(DockerCliContextResponse::current).findFirst().orElse(null);
	}

	private static String fromEndpoint(String endpoint) {
		return (StringUtils.hasLength(endpoint)) ? fromUri(URI.create(endpoint)) : null;
	}

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
