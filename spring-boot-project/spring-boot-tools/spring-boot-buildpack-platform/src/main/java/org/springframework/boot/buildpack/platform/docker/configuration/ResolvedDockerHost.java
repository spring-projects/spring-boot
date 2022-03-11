/*
 * Copyright 2012-2022 the original author or authors.
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

	ResolvedDockerHost(String address, boolean secure, String certificatePath) {
		super(address, secure, certificatePath);
	}

	@Override
	public String getAddress() {
		return super.getAddress().startsWith(UNIX_SOCKET_PREFIX)
				? super.getAddress().substring(UNIX_SOCKET_PREFIX.length()) : super.getAddress();
	}

	public boolean isRemote() {
		return getAddress().startsWith("http") || getAddress().startsWith("tcp");
	}

	public boolean isLocalFileReference() {
		try {
			return Files.exists(Paths.get(getAddress()));
		}
		catch (Exception ex) {
			return false;
		}
	}

	public static ResolvedDockerHost from(DockerHost dockerHost) {
		return from(Environment.SYSTEM, dockerHost);
	}

	static ResolvedDockerHost from(Environment environment, DockerHost dockerHost) {
		if (environment.get(DOCKER_HOST) != null) {
			return new ResolvedDockerHost(environment.get(DOCKER_HOST), isTrue(environment.get(DOCKER_TLS_VERIFY)),
					environment.get(DOCKER_CERT_PATH));
		}
		if (dockerHost != null && dockerHost.getAddress() != null) {
			return new ResolvedDockerHost(dockerHost.getAddress(), dockerHost.isSecure(),
					dockerHost.getCertificatePath());
		}
		return new ResolvedDockerHost(Platform.isWindows() ? WINDOWS_NAMED_PIPE_PATH : DOMAIN_SOCKET_PATH, false, null);
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
