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

package org.springframework.boot.cli;

import java.io.File;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.cli.command.archive.JarCommand;
import org.springframework.boot.cli.infrastructure.CommandLineInvoker;
import org.springframework.boot.cli.infrastructure.CommandLineInvoker.Invocation;
import org.springframework.boot.loader.tools.JavaExecutable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link JarCommand}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
class JarCommandIT {

	private static final boolean JAVA_9_OR_LATER = isClassPresent(
			"java.security.cert.URICertStoreParameters");

	private CommandLineInvoker cli;

	private File tempDir;

	@BeforeEach
	public void setup(@TempDir File tempDir) {
		this.cli = new CommandLineInvoker(new File("src/it/resources/jar-command"),
				tempDir);
		this.tempDir = tempDir;
	}

	@Test void noArguments() throws Exception {
		Invocation invocation = this.cli.invoke("jar");
		invocation.await();
		assertThat(invocation.getStandardOutput()).isEqualTo("");
		assertThat(invocation.getErrorOutput()).contains("The name of the "
				+ "resulting jar and at least one source file must be specified");
	}

	@Test void noSources() throws Exception {
		Invocation invocation = this.cli.invoke("jar", "test-app.jar");
		invocation.await();
		assertThat(invocation.getStandardOutput()).isEqualTo("");
		assertThat(invocation.getErrorOutput()).contains("The name of the "
				+ "resulting jar and at least one source file must be specified");
	}

	@Test void jarCreationWithGrabResolver() throws Exception {
		File jar = new File(this.tempDir, "test-app.jar");
		Invocation invocation = this.cli.invoke("run", jar.getAbsolutePath(),
				"bad.groovy");
		invocation.await();
		if (!JAVA_9_OR_LATER) {
			assertThat(invocation.getErrorOutput()).isEqualTo("");
		}
		invocation = this.cli.invoke("jar", jar.getAbsolutePath(), "bad.groovy");
		invocation.await();
		if (!JAVA_9_OR_LATER) {
			assertThat(invocation.getErrorOutput()).isEmpty();
		}
		assertThat(jar).exists();

		Process process = new JavaExecutable()
				.processBuilder("-jar", jar.getAbsolutePath()).start();
		invocation = new Invocation(process);
		invocation.await();

		if (!JAVA_9_OR_LATER) {
			assertThat(invocation.getErrorOutput()).isEqualTo("");
		}
	}

	@Test void jarCreation() throws Exception {
		File jar = new File(this.tempDir, "test-app.jar");
		Invocation invocation = this.cli.invoke("jar", jar.getAbsolutePath(),
				"jar.groovy");
		invocation.await();
		if (!JAVA_9_OR_LATER) {
			assertThat(invocation.getErrorOutput()).isEmpty();
		}
		assertThat(jar).exists();

		Process process = new JavaExecutable()
				.processBuilder("-jar", jar.getAbsolutePath()).start();
		invocation = new Invocation(process);
		invocation.await();

		if (!JAVA_9_OR_LATER) {
			assertThat(invocation.getErrorOutput()).isEqualTo("");
		}
		assertThat(invocation.getStandardOutput()).contains("Hello World!")
				.contains("/BOOT-INF/classes!/public/public.txt")
				.contains("/BOOT-INF/classes!/resources/resource.txt")
				.contains("/BOOT-INF/classes!/static/static.txt")
				.contains("/BOOT-INF/classes!/templates/template.txt")
				.contains("/BOOT-INF/classes!/root.properties").contains("Goodbye Mama");
	}

	@Test void jarCreationWithIncludes() throws Exception {
		File jar = new File(this.tempDir, "test-app.jar");
		Invocation invocation = this.cli.invoke("jar", jar.getAbsolutePath(), "--include",
				"-public/**,-resources/**", "jar.groovy");
		invocation.await();
		if (!JAVA_9_OR_LATER) {
			assertThat(invocation.getErrorOutput()).isEmpty();
		}
		assertThat(jar).exists();

		Process process = new JavaExecutable()
				.processBuilder("-jar", jar.getAbsolutePath()).start();
		invocation = new Invocation(process);
		invocation.await();

		if (!JAVA_9_OR_LATER) {
			assertThat(invocation.getErrorOutput()).isEqualTo("");
		}
		assertThat(invocation.getStandardOutput()).contains("Hello World!")
				.doesNotContain("/public/public.txt")
				.doesNotContain("/resources/resource.txt").contains("/static/static.txt")
				.contains("/templates/template.txt").contains("Goodbye Mama");
	}

	private static boolean isClassPresent(String name) {
		try {
			Class.forName(name);
			return true;
		}
		catch (Exception ex) {
			return false;
		}
	}

}
