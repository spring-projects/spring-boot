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

package org.springframework.boot.maven;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.loader.tools.JavaExecutable;
import org.springframework.boot.maven.sample.ClassWithMainMethod;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CommandLineBuilder}.
 *
 * @author Stephane Nicoll
 */
class CommandLineBuilderTests {

	public static final String CLASS_NAME = ClassWithMainMethod.class.getName();

	@Test
	void buildWithNullJvmArgumentsIsIgnored() {
		assertThat(CommandLineBuilder.forMainClass(CLASS_NAME).withJvmArguments((String[]) null).build())
			.containsExactly(CLASS_NAME);
	}

	@Test
	void buildWithNullIntermediateJvmArgumentIsIgnored() {
		assertThat(CommandLineBuilder.forMainClass(CLASS_NAME)
			.withJvmArguments("-verbose:class", null, "-verbose:gc")
			.build()).containsExactly("-verbose:class", "-verbose:gc", CLASS_NAME);
	}

	@Test
	void buildWithJvmArgument() {
		assertThat(CommandLineBuilder.forMainClass(CLASS_NAME).withJvmArguments("-verbose:class").build())
			.containsExactly("-verbose:class", CLASS_NAME);
	}

	@Test
	void buildWithNullSystemPropertyIsIgnored() {
		assertThat(CommandLineBuilder.forMainClass(CLASS_NAME).withSystemProperties(null).build())
			.containsExactly(CLASS_NAME);
	}

	@Test
	void buildWithSystemProperty() {
		assertThat(CommandLineBuilder.forMainClass(CLASS_NAME).withSystemProperties(Map.of("flag", "enabled")).build())
			.containsExactly("-Dflag=\"enabled\"", CLASS_NAME);
	}

	@Test
	void buildWithNullArgumentsIsIgnored() {
		assertThat(CommandLineBuilder.forMainClass(CLASS_NAME).withArguments((String[]) null).build())
			.containsExactly(CLASS_NAME);
	}

	@Test
	void buildWithNullIntermediateArgumentIsIgnored() {
		assertThat(CommandLineBuilder.forMainClass(CLASS_NAME).withArguments("--test", null, "--another").build())
			.containsExactly(CLASS_NAME, "--test", "--another");
	}

	@Test
	@DisabledOnOs(OS.WINDOWS)
	void buildWithClassPath(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("test.jar");
		Path file1 = tempDir.resolve("test1.jar");
		assertThat(CommandLineBuilder.forMainClass(CLASS_NAME)
			.withClasspath(file.toUri().toURL(), file1.toUri().toURL())
			.build()).containsExactly("-cp", file + File.pathSeparator + file1, CLASS_NAME);
	}

	@Test
	@EnabledOnOs(OS.WINDOWS)
	void buildWithClassPathOnWindows(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("test.jar");
		Path file1 = tempDir.resolve("test1.jar");
		List<String> args = CommandLineBuilder.forMainClass(CLASS_NAME)
			.withClasspath(file.toUri().toURL(), file1.toUri().toURL())
			.build();
		assertThat(args).hasSize(3);
		assertThat(args.get(0)).isEqualTo("-cp");
		assertThat(args.get(1)).startsWith("@");
		assertThat(args.get(2)).isEqualTo(CLASS_NAME);
		assertThat(Paths.get(args.get(1).substring(1)))
			.hasContent("\"" + (file + File.pathSeparator + file1).replace("\\", "\\\\") + "\"");
	}

	@Test
	void buildAndRunWithLongClassPath() throws IOException, InterruptedException {
		StringBuilder classPath = new StringBuilder(ManagementFactory.getRuntimeMXBean().getClassPath());
		// Simulates [CreateProcess error=206, The filename or extension is too long]
		while (classPath.length() < 35000) {
			classPath.append(File.pathSeparator).append(classPath);
		}
		URL[] urls = Arrays.stream(classPath.toString().split(File.pathSeparator)).map(this::toURL).toArray(URL[]::new);
		List<String> command = CommandLineBuilder.forMainClass(ClassWithMainMethod.class.getName())
			.withClasspath(urls)
			.build();
		ProcessBuilder pb = new JavaExecutable().processBuilder(command.toArray(new String[0]));
		Process process = pb.start();
		assertThat(process.waitFor()).isEqualTo(0);
		try (InputStream inputStream = process.getInputStream()) {
			assertThat(inputStream).hasContent("Hello World");
		}
	}

	private URL toURL(String path) {
		try {
			return Paths.get(path).toUri().toURL();
		}
		catch (MalformedURLException ex) {
			throw new RuntimeException(ex);
		}
	}

}
