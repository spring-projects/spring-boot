/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.List;

import org.junit.rules.ExternalResource;

import org.springframework.util.FileCopyUtils;

/**
 * Base {@link ExternalResource} for launching a Spring Boot application as part of a
 * JUnit test.
 *
 * @author Andy Wilkinson
 */
abstract class AbstractApplicationLauncher extends ExternalResource {

	private final ApplicationBuilder applicationBuilder;

	private Process process;

	private int httpPort;

	protected AbstractApplicationLauncher(ApplicationBuilder applicationBuilder) {
		this.applicationBuilder = applicationBuilder;
	}

	@Override
	protected final void before() throws Throwable {
		this.process = startApplication();
	}

	@Override
	protected final void after() {
		this.process.destroy();
	}

	public final int getHttpPort() {
		return this.httpPort;
	}

	protected abstract List<String> getArguments(File archive);

	protected abstract File getWorkingDirectory();

	protected abstract String getDescription(String packaging);

	private Process startApplication() throws Exception {
		File workingDirectory = getWorkingDirectory();
		File serverPortFile = workingDirectory == null ? new File("target/server.port")
				: new File(workingDirectory, "target/server.port");
		serverPortFile.delete();
		File archive = this.applicationBuilder.buildApplication();
		List<String> arguments = new ArrayList<>();
		arguments.add(System.getProperty("java.home") + "/bin/java");
		arguments.addAll(getArguments(archive));
		ProcessBuilder processBuilder = new ProcessBuilder(
				arguments.toArray(new String[arguments.size()]));
		processBuilder.redirectOutput(Redirect.INHERIT);
		processBuilder.redirectError(Redirect.INHERIT);
		if (workingDirectory != null) {
			processBuilder.directory(workingDirectory);
		}
		Process process = processBuilder.start();
		this.httpPort = awaitServerPort(process, serverPortFile);
		return process;
	}

	private int awaitServerPort(Process process, File serverPortFile) throws Exception {
		long end = System.currentTimeMillis() + 30000;
		while (serverPortFile.length() == 0) {
			if (System.currentTimeMillis() > end) {
				throw new IllegalStateException(
						"server.port file was not written within 30 seconds");
			}
			if (!process.isAlive()) {
				throw new IllegalStateException("Application failed to launch");
			}
			Thread.sleep(100);
		}
		return Integer
				.parseInt(FileCopyUtils.copyToString(new FileReader(serverPortFile)));
	}

}
