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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.sun.jna.Platform;

import org.springframework.boot.buildpack.platform.json.SharedObjectMapper;

/**
 * Default implementation of the {@link DockerCredentialHelper} that retrieves Docker
 * credentials using a specified credential helper.
 *
 * @author Dmytro Nosan
 */
class DefaultDockerCredentialHelper implements DockerCredentialHelper {

	private static final String USR_LOCAL_BIN = "/usr/local/bin/";

	private static final String CREDENTIALS_NOT_FOUND = "credentials not found in native keychain";

	private static final String CREDENTIALS_URL_MISSING = "no credentials server URL";

	private static final String CREDENTIALS_USERNAME_MISSING = "no credentials username";

	private final String name;

	/**
	 * Creates a new {@link DefaultDockerCredentialHelper} instance using the specified
	 * credential helper name.
	 * @param name the full name of the Docker credential helper, e.g.,
	 * {@code docker-credential-osxkeychain}, {@code docker-credential-desktop}, etc.
	 */
	DefaultDockerCredentialHelper(String name) {
		this.name = name;
	}

	@Override
	public Credentials get(String serverUrl) throws IOException {
		ProcessBuilder processBuilder = new ProcessBuilder().redirectErrorStream(true);
		if (Platform.isWindows()) {
			processBuilder.command("cmd", "/c");
		}
		processBuilder.command(this.name, "get");
		Process process = startProcess(processBuilder);
		try (OutputStream os = process.getOutputStream()) {
			os.write(serverUrl.getBytes(StandardCharsets.UTF_8));
		}
		int exitCode;
		try {
			exitCode = process.waitFor();
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			return null;
		}
		if (exitCode != 0) {
			try (InputStream is = process.getInputStream()) {
				String message = new String(is.readAllBytes(), StandardCharsets.UTF_8);
				if (isCredentialsNotFoundError(message)) {
					return null;
				}
				throw new IOException("%s' exited with code %d: %s".formatted(process, exitCode, message));
			}
		}
		try (InputStream is = process.getInputStream()) {
			return new Credentials(SharedObjectMapper.get().readTree(is));
		}
	}

	private Process startProcess(ProcessBuilder processBuilder) throws IOException {
		try {
			return processBuilder.start();
		}
		catch (IOException ex) {
			if (Platform.isMac()) {
				try {
					List<String> command = new ArrayList<>(processBuilder.command());
					command.set(0, USR_LOCAL_BIN + command.get(0));
					return processBuilder.command(command).start();
				}
				catch (IOException ignore) {
					// Ignore, rethrow the original exception
				}
			}
			throw ex;
		}
	}

	private boolean isCredentialsNotFoundError(String message) {
		return switch (message.trim()) {
			case CREDENTIALS_NOT_FOUND, CREDENTIALS_URL_MISSING, CREDENTIALS_USERNAME_MISSING -> true;
			default -> false;
		};
	}

}
