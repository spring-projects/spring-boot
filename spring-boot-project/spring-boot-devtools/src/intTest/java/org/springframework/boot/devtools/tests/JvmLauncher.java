/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.devtools.tests;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

import org.springframework.boot.testsupport.BuildOutput;
import org.springframework.util.StringUtils;

/**
 * {@link Extension} that launches a JVM and redirects its output to a test
 * method-specific location.
 *
 * @author Andy Wilkinson
 */
class JvmLauncher implements BeforeTestExecutionCallback {

	private static final Pattern NON_ALPHABET_PATTERN = Pattern.compile("[^A-Za-z]+");

	private final BuildOutput buildOutput = new BuildOutput(getClass());

	private File outputDirectory;

	@Override
	public void beforeTestExecution(ExtensionContext context) throws Exception {
		this.outputDirectory = new File(this.buildOutput.getRootLocation(),
				"output/" + NON_ALPHABET_PATTERN.matcher(context.getRequiredTestMethod().getName()).replaceAll(""));
		this.outputDirectory.mkdirs();
	}

	LaunchedJvm launch(String name, String classpath, String... args) throws IOException {
		List<String> command = new ArrayList<>(
				Arrays.asList(System.getProperty("java.home") + "/bin/java", "-cp", classpath));
		command.addAll(Arrays.asList(args));
		File standardOut = new File(this.outputDirectory, name + ".out");
		File standardError = new File(this.outputDirectory, name + ".err");
		Process process = new ProcessBuilder(StringUtils.toStringArray(command)).redirectError(standardError)
				.redirectOutput(standardOut).start();
		return new LaunchedJvm(process, standardOut, standardError);
	}

	static class LaunchedJvm {

		private final Process process;

		private final Instant launchTime = Instant.now();

		private final File standardOut;

		private final File standardError;

		LaunchedJvm(Process process, File standardOut, File standardError) {
			this.process = process;
			this.standardOut = standardOut;
			this.standardError = standardError;
		}

		Process getProcess() {
			return this.process;
		}

		Instant getLaunchTime() {
			return this.launchTime;
		}

		File getStandardOut() {
			return this.standardOut;
		}

		File getStandardError() {
			return this.standardError;
		}

	}

}
