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

import java.io.IOException;

/**
 * Docker credential helper used to retrieve credentials for servers.
 *
 * @author Dmytro Nosan
 */
interface DockerCredentialHelper {

	/**
	 * Retrieves the credential associated with the specified URL.
	 * @param serverUrl the server URL for which the credential is requested
	 * @return the {@link Credentials} containing authentication information for the given
	 * server, or {@code null} if no credential is available for the given server.
	 * @throws IOException if an I/O error occurs while retrieving the credential.
	 */
	Credentials get(String serverUrl) throws IOException;

	/**
	 * Creates a {@link DockerCredentialHelper} instance using the specified suffix.
	 * @param suffix the suffix of the credential helper, for example {@code gcr},
	 * {@code ecr-login}, {@code desktop}, {@code osxkeychain}, etc.
	 * @return a {@link DefaultDockerCredentialHelper} instance, with the full name of the
	 * helper. e.g., {@code docker-credential-gcr}
	 */
	static DockerCredentialHelper ofSuffix(String suffix) {
		return new DefaultDockerCredentialHelper("docker-credential-" + suffix.trim());
	}

}
