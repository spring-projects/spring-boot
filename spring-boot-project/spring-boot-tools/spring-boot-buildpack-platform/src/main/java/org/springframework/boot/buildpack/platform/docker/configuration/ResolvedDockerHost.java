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

package org.springframework.boot.buildpack.platform.docker.configuration;

import java.nio.file.Files;
import java.nio.file.Paths;

import com.sun.jna.Platform;

import org.springframework.boot.buildpack.platform.docker.configuration.DockerConfiguration.DockerHostConfiguration;
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

	/**
	 * Constructs a new ResolvedDockerHost object with the specified address.
	 * @param address the address of the Docker host
	 */
	ResolvedDockerHost(String address) {
		super(address);
	}

	/**
	 * Constructs a new ResolvedDockerHost object with the specified address, secure flag,
	 * and certificate path.
	 * @param address the address of the Docker host
	 * @param secure a boolean flag indicating whether the connection to the Docker host
	 * should be secure
	 * @param certificatePath the path to the certificate file for secure connection
	 */
	ResolvedDockerHost(String address, boolean secure, String certificatePath) {
		super(address, secure, certificatePath);
	}

	/**
	 * Returns the address of the resolved Docker host. If the address starts with the
	 * UNIX_SOCKET_PREFIX, it will be stripped off before returning.
	 * @return the address of the resolved Docker host
	 */
	@Override
	public String getAddress() {
		return super.getAddress().startsWith(UNIX_SOCKET_PREFIX)
				? super.getAddress().substring(UNIX_SOCKET_PREFIX.length()) : super.getAddress();
	}

	/**
	 * Checks if the address of the Docker host is remote.
	 * @return true if the address starts with "http" or "tcp", indicating a remote host,
	 * false otherwise.
	 */
	public boolean isRemote() {
		return getAddress().startsWith("http") || getAddress().startsWith("tcp");
	}

	/**
	 * Checks if the address of the file is a local file reference.
	 * @return true if the address is a local file reference, false otherwise
	 */
	public boolean isLocalFileReference() {
		try {
			return Files.exists(Paths.get(getAddress()));
		}
		catch (Exception ex) {
			return false;
		}
	}

	/**
	 * Creates a ResolvedDockerHost object from the given DockerHostConfiguration object.
	 * @param dockerHost the DockerHostConfiguration object to create the
	 * ResolvedDockerHost from
	 * @return a ResolvedDockerHost object representing the resolved Docker host
	 */
	public static ResolvedDockerHost from(DockerHostConfiguration dockerHost) {
		return from(Environment.SYSTEM, dockerHost);
	}

	/**
	 * Resolves the Docker host based on the given environment and Docker host
	 * configuration.
	 * @param environment The environment containing the Docker host information.
	 * @param dockerHost The Docker host configuration.
	 * @return The resolved Docker host.
	 */
	static ResolvedDockerHost from(Environment environment, DockerHostConfiguration dockerHost) {
		DockerConfigurationMetadata config = DockerConfigurationMetadata.from(environment);
		if (environment.get(DOCKER_CONTEXT) != null) {
			DockerContext context = config.forContext(environment.get(DOCKER_CONTEXT));
			return new ResolvedDockerHost(context.getDockerHost(), context.isTlsVerify(), context.getTlsPath());
		}
		if (dockerHost != null && dockerHost.getContext() != null) {
			DockerContext context = config.forContext(dockerHost.getContext());
			return new ResolvedDockerHost(context.getDockerHost(), context.isTlsVerify(), context.getTlsPath());
		}
		if (environment.get(DOCKER_HOST) != null) {
			return new ResolvedDockerHost(environment.get(DOCKER_HOST), isTrue(environment.get(DOCKER_TLS_VERIFY)),
					environment.get(DOCKER_CERT_PATH));
		}
		if (dockerHost != null && dockerHost.getAddress() != null) {
			return new ResolvedDockerHost(dockerHost.getAddress(), dockerHost.isSecure(),
					dockerHost.getCertificatePath());
		}
		if (config.getContext().getDockerHost() != null) {
			DockerContext context = config.getContext();
			return new ResolvedDockerHost(context.getDockerHost(), context.isTlsVerify(), context.getTlsPath());
		}
		return new ResolvedDockerHost(Platform.isWindows() ? WINDOWS_NAMED_PIPE_PATH : DOMAIN_SOCKET_PATH);
	}

	/**
	 * Checks if the given value is true.
	 * @param value the value to be checked
	 * @return true if the value is not null and equals to 1, false otherwise
	 */
	private static boolean isTrue(String value) {
		try {
			return (value != null) && (Integer.parseInt(value) == 1);
		}
		catch (NumberFormatException ex) {
			return false;
		}
	}

}
