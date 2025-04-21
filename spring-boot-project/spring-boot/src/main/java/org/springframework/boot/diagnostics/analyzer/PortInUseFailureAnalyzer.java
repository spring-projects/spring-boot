/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.diagnostics.analyzer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.web.server.PortInUseException;

/**
 * A {@code FailureAnalyzer} that performs analysis of failures caused by a
 * {@code PortInUseException}. <br/>
 * The analyzer attempts to find the process that is using the port and provides
 * information about it in the failure analysis.
 *
 * @author Andy Wilkinson
 * @author Yonatan Graber
 */
class PortInUseFailureAnalyzer extends AbstractFailureAnalyzer<PortInUseException> {

	private static final Log logger = LogFactory.getLog(PortInUseFailureAnalyzer.class);

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, PortInUseException cause) {
		ProcessInfo processInfo = findProcessUsingPort(cause.getPort());

		String description = buildDescription(cause.getPort(), processInfo);
		String action = buildAction(cause.getPort(), processInfo);
		return new FailureAnalysis(description, action, cause);
	}

	private String buildDescription(int port, ProcessInfo processInfo) {
		StringBuilder message = new StringBuilder();
		message.append("Web server failed to start. Port ").append(port).append(" was already in use");
		if (processInfo != null) {
			message.append(" by ").append(processInfo.command).append(" (PID: ").append(processInfo.pid).append(")");
		}
		message.append(".");
		return message.toString();
	}

	private String buildAction(int port, ProcessInfo processInfo) {
		StringBuilder message = new StringBuilder();
		if (processInfo != null) {
			message.append("Stop the process ")
				.append(processInfo.command)
				.append(" (PID: ")
				.append(processInfo.pid)
				.append(")");
		}
		else {
			message.append("Identify and stop the process");
		}
		message.append(" that's listening on port ")
			.append(port)
			.append(" or configure this application to listen on another port.");
		return message.toString();
	}

	/**
	 * Find the process using the given port. Will invoke OS-specific commands to
	 * determine the process ID and command name.
	 * @param port the port to check
	 * @return the process information or {@code null} if the process cannot be found for
	 * any reason
	 */
	private ProcessInfo findProcessUsingPort(int port) {
		String os = System.getProperty("os.name").toLowerCase();
		try {
			if (os.contains("win")) {
				return findProcessOnWindows(port);
			}
			else if (os.contains("mac") || os.contains("nix") || os.contains("nux")) {
				return findProcessOnUnix(port);
			}
			else {
				logger.debug("Could not find process using port " + port + " in OS " + os);
			}
		}
		catch (Exception ex) {
			logger.warn("Unable to find process using port " + port, ex);
		}
		return null;
	}

	private ProcessInfo findProcessOnWindows(int port) throws Exception {
		Process process = new ProcessBuilder("cmd.exe", "/c", "netstat -ano | findstr :" + port).start();
		waitForProcess(process);
		List<String> lines = readOutput(process);
		for (String line : lines) {
			line = line.trim();
			if (line.contains("LISTENING") || line.contains("ESTABLISHED")) {
				String[] parts = line.split("\\s+");
				if (parts.length >= 5) {
					String pid = parts[4];
					String command = getWindowsCommandByPid(pid);
					return new ProcessInfo(pid, command);
				}
			}
		}
		return null;
	}

	private String getWindowsCommandByPid(String pid) throws Exception {
		Process process = new ProcessBuilder("cmd.exe", "/c", "tasklist /FI \"PID eq " + pid + "\"").start();
		waitForProcess(process);
		List<String> lines = readOutput(process);
		for (String line : lines) {
			if (line.startsWith("Image Name")) {
				continue;
			}
			if (line.toLowerCase().contains(pid)) {
				return line.split("\\s+")[0];
			}
		}
		return null;
	}

	private ProcessInfo findProcessOnUnix(int port) throws IOException {
		Process process = new ProcessBuilder("lsof", "-nP", "-i", ":" + port).start();
		waitForProcess(process);
		List<String> lines = readOutput(process);
		for (String line : lines) {
			if (line.startsWith("COMMAND")) {
				continue; // header
			}
			String[] parts = line.trim().split("\\s+");
			if (parts.length >= 2) {
				return new ProcessInfo(parts[1], parts[0]);
			}
		}
		return null;
	}

	private void waitForProcess(Process process) throws IOException {
		try {
			if (!process.waitFor(1, TimeUnit.SECONDS)) {
				process.destroy();
				throw new IOException("Process timed out");
			}
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IOException("Process interrupted", ex);
		}
	}

	private List<String> readOutput(Process process) throws IOException {
		List<String> lines = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				lines.add(line);
			}
		}
		return lines;
	}

	private record ProcessInfo(String pid, String command) {
	}

}
