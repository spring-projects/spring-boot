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

package org.springframework.boot.buildpack.platform.docker.configuration;

import java.nio.file.Files;
import java.nio.file.Paths;

import com.sun.jna.Platform;

import org.springframework.boot.buildpack.platform.docker.configuration.DockerConfigurationMetadata.DockerContext;
import org.springframework.boot.buildpack.platform.system.Environment;

/**
 * Resolves a {@link DockerHost} from the environment, configuration, or using defaults.
 *
 * @author Scott Frederick
 * @since 2.7.0
 */
public class ResolvedDockerHost extends DockerHost {

	private static final String UNIX_SOCKET_PREFIX = "unix://";

	private static final String DOMAIN_SOCKET_PATH = "/var/run/docker.sock";

	private static final String WINDOWS_NAMED_PIPE_PATH = "//./pipe/docker_engine";

	private static final String DOCKER_HOST = "DOCKER_HOST";

	private static final String DOCKER_TLS_VERIFY = "DOCKER_TLS_VERIFY";

	private static final String DOCKER_CERT_PATH = "DOCKER_CERT_PATH";

	private static final String DOCKER_CONTEXT = "DOCKER_CONTEXT";

	ResolvedDockerHost(String address) {
		super(address);
	}

	ResolvedDockerHost(String address, boolean secure, String certificatePath) {
		super(address, secure, certificatePath);
	}

	@Override
	public String getAddress() {
		String address = super.getAddress();
		if (address == null) {
			address = getDefaultAddress();
		}
		return address.startsWith(UNIX_SOCKET_PREFIX) ? address.substring(UNIX_SOCKET_PREFIX.length()) : address;
	}

	public boolean isRemote() {
		String originalAddress = super.getAddress();
		if (originalAddress == null) {
			originalAddress = getDefaultAddress();
		}
		// If it starts with unix://, it's definitely local
		if (originalAddress.startsWith(UNIX_SOCKET_PREFIX)) {
			return false;
		}
		// Check the processed address for http/tcp 
		String processedAddress = getAddress();
		if (processedAddress.startsWith("http") || processedAddress.startsWith("tcp")) {
			// Check if it's localhost or 127.0.0.1 - these are local even over TCP
			if (processedAddress.contains("localhost") || processedAddress.contains("127.0.0.1")) {
				return false;
			}
			return true;
		}
		// If it's not http/tcp and it's a local file reference, it's local
		if (isLocalFileReference()) {
			return false;
		}
		// Default to remote for anything else
		return false;
	}

	public boolean isLocalFileReference() {
		try {
			return Files.exists(Paths.get(getAddress()));
		}
		catch (Exception ex) {
			return false;
		}
	}

	/**
	 * Create a new {@link ResolvedDockerHost} from the given host configuration.
	 * @param dockerHostConfiguration the host configuration or {@code null}
	 * @return the resolved docker host
	 * @deprecated since 3.5.0 for removal in 4.0.0 in favor of
	 * {@link #from(DockerConnectionConfiguration)}
	 */
	@Deprecated(since = "3.5.0", forRemoval = true)
	@SuppressWarnings("removal")
	public static ResolvedDockerHost from(DockerConfiguration.DockerHostConfiguration dockerHostConfiguration) {
		return from(Environment.SYSTEM,
				DockerConfiguration.DockerHostConfiguration.asConnectionConfiguration(dockerHostConfiguration));
	}

	/**
	 * Create a new {@link ResolvedDockerHost} from the given host configuration.
	 * @param connectionConfiguration the host configuration or {@code null}
	 * @return the resolved docker host
	 */
	public static ResolvedDockerHost from(DockerConnectionConfiguration connectionConfiguration) {
		return from(Environment.SYSTEM, connectionConfiguration);
	}

	static ResolvedDockerHost from(Environment environment, DockerConnectionConfiguration connectionConfiguration) {
		DockerConfigurationMetadata environmentConfiguration = DockerConfigurationMetadata.from(environment);
		if (environment.get(DOCKER_CONTEXT) != null) {
			DockerContext context = environmentConfiguration.forContext(environment.get(DOCKER_CONTEXT));
			return new ResolvedDockerHost(context.getDockerHost(), context.isTlsVerify(), context.getTlsPath());
		}
		if (connectionConfiguration instanceof DockerConnectionConfiguration.Context contextConfiguration) {
			DockerContext context = environmentConfiguration.forContext(contextConfiguration.context());
			return new ResolvedDockerHost(context.getDockerHost(), context.isTlsVerify(), context.getTlsPath());
		}
		if (environment.get(DOCKER_HOST) != null) {
			return new ResolvedDockerHost(environment.get(DOCKER_HOST), isTrue(environment.get(DOCKER_TLS_VERIFY)),
					environment.get(DOCKER_CERT_PATH));
		}
		if (connectionConfiguration instanceof DockerConnectionConfiguration.Host addressConfiguration) {
			return new ResolvedDockerHost(addressConfiguration.address(), addressConfiguration.secure(),
					addressConfiguration.certificatePath());
		}
		if (environmentConfiguration.getContext().getDockerHost() != null) {
			DockerContext context = environmentConfiguration.getContext();
			return new ResolvedDockerHost(context.getDockerHost(), context.isTlsVerify(), context.getTlsPath());
		}
		return new ResolvedDockerHost(getDefaultAddress());
	}

	private static String getDefaultAddress() {
		return Platform.isWindows() ? WINDOWS_NAMED_PIPE_PATH : DOMAIN_SOCKET_PATH;
	}

	private static boolean isTrue(String value) {
		try {
			return (value != null) && (Integer.parseInt(value) == 1);
		}
		catch (NumberFormatException ex) {
			return false;
		}
	}

}
