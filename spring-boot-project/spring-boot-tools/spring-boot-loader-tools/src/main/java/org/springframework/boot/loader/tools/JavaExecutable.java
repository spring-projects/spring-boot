/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.loader.tools;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Provides access to the java binary executable, regardless of OS.
 *
 * @author Phillip Webb
 * @since 1.1.0
 */
public class JavaExecutable {

	private File file;

	public JavaExecutable() {
		String javaHome = System.getProperty("java.home");
		Assert.state(StringUtils.hasLength(javaHome),
				"Unable to find java executable due to missing 'java.home'");
		this.file = findInJavaHome(javaHome);
	}

	private File findInJavaHome(String javaHome) {
		File bin = new File(new File(javaHome), "bin");
		File command = new File(bin, "java.exe");
		command = command.exists() ? command : new File(bin, "java");
		Assert.state(command.exists(), () -> "Unable to find java in " + javaHome);
		return command;
	}

	/**
	 * Create a new {@link ProcessBuilder} that will run with the Java executable.
	 * @param arguments the command arguments
	 * @return a {@link ProcessBuilder}
	 */
	public ProcessBuilder processBuilder(String... arguments) {
		ProcessBuilder processBuilder = new ProcessBuilder(toString());
		processBuilder.command().addAll(Arrays.asList(arguments));
		return processBuilder;
	}

	@Override
	public String toString() {
		try {
			return this.file.getCanonicalPath();
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
