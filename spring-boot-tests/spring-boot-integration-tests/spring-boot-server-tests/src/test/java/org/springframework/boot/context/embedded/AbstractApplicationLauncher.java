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

package org.springframework.boot.context.embedded;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import org.springframework.boot.testsupport.BuildOutput;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * Base class for launching a Spring Boot application as part of a JUnit test.
 *
 * @author Andy Wilkinson
 */
abstract class AbstractApplicationLauncher implements BeforeEachCallback, AfterEachCallback {

	private final ApplicationBuilder applicationBuilder;

	private final BuildOutput buildOutput;

	private Process process;

	private int httpPort;

	protected AbstractApplicationLauncher(ApplicationBuilder applicationBuilder, BuildOutput buildOutput) {
		this.applicationBuilder = applicationBuilder;
		this.buildOutput = buildOutput;
	}

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		this.process.destroy();
	}

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		this.process = startApplication();
	}

	final int getHttpPort() {
		return this.httpPort;
	}

	protected abstract List<String> getArguments(File archive, File serverPortFile);

	protected abstract File getWorkingDirectory();

	protected abstract String getDescription(String packaging);

	private Process startApplication() throws Exception {
		File workingDirectory = getWorkingDirectory();
		File serverPortFile = new File(this.buildOutput.getRootLocation(), "server.port");
		serverPortFile.delete();
		File archive = this.applicationBuilder.buildApplication();
		List<String> arguments = new ArrayList<>();
		arguments.add(System.getProperty("java.home") + "/bin/java");
		arguments.addAll(getArguments(archive, serverPortFile));
		ProcessBuilder processBuilder = new ProcessBuilder(StringUtils.toStringArray(arguments));
		if (workingDirectory != null) {
			processBuilder.directory(workingDirectory);
		}
		Process process = processBuilder.start();
		new ConsoleCopy(process.getInputStream(), System.out).start();
		new ConsoleCopy(process.getErrorStream(), System.err).start();
		this.httpPort = awaitServerPort(process, serverPortFile);
		return process;
	}

	private int awaitServerPort(Process process, File serverPortFile) throws Exception {
		Awaitility.waitAtMost(Duration.ofSeconds(30)).until(serverPortFile::length, (length) -> {
			if (!process.isAlive()) {
				throw new IllegalStateException("Application failed to start");
			}
			return length > 0;
		});
		return Integer.parseInt(FileCopyUtils.copyToString(new FileReader(serverPortFile)));
	}

	private static class ConsoleCopy extends Thread {

		private final InputStream input;

		private final PrintStream output;

		ConsoleCopy(InputStream input, PrintStream output) {
			this.input = input;
			this.output = output;
		}

		@Override
		public void run() {
			try {
				StreamUtils.copy(this.input, this.output);
			}
			catch (IOException ex) {
			}
		}

	}

}
