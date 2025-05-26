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
import java.util.Set;

import com.sun.jna.Platform;

import org.springframework.boot.buildpack.platform.json.SharedObjectMapper;

/**
 * Invokes a Docker credential helper executable that can be used to get {@link Credential
 * credentials}.
 *
 * @author Dmytro Nosan
 * @author Phillip Webb
 */
class CredentialHelper {

	private static final String USR_LOCAL_BIN = "/usr/local/bin/";

	private static final Set<String> CREDENTIAL_NOT_FOUND_MESSAGES = Set.of("credentials not found in native keychain",
			"no credentials server URL", "no credentials username");

	private final String executable;

	CredentialHelper(String executable) {
		this.executable = executable;
	}

	Credential get(String serverUrl) throws IOException {
		ProcessBuilder processBuilder = processBuilder("get");
		Process process = start(processBuilder);
		try (OutputStream request = process.getOutputStream()) {
			request.write(serverUrl.getBytes(StandardCharsets.UTF_8));
		}
		try {
			int exitCode = process.waitFor();
			try (InputStream response = process.getInputStream()) {
				if (exitCode == 0) {
					return new Credential(SharedObjectMapper.get().readTree(response));
				}
				String errorMessage = new String(response.readAllBytes(), StandardCharsets.UTF_8);
				if (!isCredentialsNotFoundError(errorMessage)) {
					throw new IOException("%s' exited with code %d: %s".formatted(process, exitCode, errorMessage));
				}
				return null;
			}
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			return null;
		}
	}

	private ProcessBuilder processBuilder(String action) {
		ProcessBuilder processBuilder = new ProcessBuilder().redirectErrorStream(true);
		if (Platform.isWindows()) {
			processBuilder.command("cmd", "/c");
		}
		processBuilder.command(this.executable, action);
		return processBuilder;
	}

	private Process start(ProcessBuilder processBuilder) throws IOException {
		try {
			return processBuilder.start();
		}
		catch (IOException ex) {
			if (!Platform.isMac()) {
				throw ex;
			}
			try {
				List<String> command = new ArrayList<>(processBuilder.command());
				command.set(0, USR_LOCAL_BIN + command.get(0));
				return processBuilder.command(command).start();
			}
			catch (Exception suppressed) {
				// Suppresses the exception and rethrows the original exception
				ex.addSuppressed(suppressed);
				throw ex;
			}
		}
	}

	private static boolean isCredentialsNotFoundError(String message) {
		return CREDENTIAL_NOT_FOUND_MESSAGES.contains(message.trim());
	}

}
