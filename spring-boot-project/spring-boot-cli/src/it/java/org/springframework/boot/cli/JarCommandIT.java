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

import org.junit.Test;

import org.springframework.boot.cli.command.archive.JarCommand;
import org.springframework.boot.cli.infrastructure.CommandLineInvoker;
import org.springframework.boot.cli.infrastructure.CommandLineInvoker.Invocation;
import org.springframework.boot.loader.tools.JavaExecutable;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Integration test for {@link JarCommand}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
public class JarCommandIT {

	private static final boolean JAVA_9_OR_LATER = isClassPresent(
			"java.security.cert.URICertStoreParameters");

	private final CommandLineInvoker cli = new CommandLineInvoker(
			new File("src/it/resources/jar-command"));

	@Test
	public void noArguments() throws Exception {
		Invocation invocation = this.cli.invoke("jar");
		invocation.await();
		assertThat(invocation.getStandardOutput(), equalTo(""));
		assertThat(invocation.getErrorOutput(), containsString("The name of the "
				+ "resulting jar and at least one source file must be specified"));
	}

	@Test
	public void noSources() throws Exception {
		Invocation invocation = this.cli.invoke("jar", "test-app.jar");
		invocation.await();
		assertThat(invocation.getStandardOutput(), equalTo(""));
		assertThat(invocation.getErrorOutput(), containsString("The name of the "
				+ "resulting jar and at least one source file must be specified"));
	}

	@Test
	public void jarCreationWithGrabResolver() throws Exception {
		File jar = new File("target/test-app.jar");
		Invocation invocation = this.cli.invoke("run", jar.getAbsolutePath(),
				"bad.groovy");
		invocation.await();
		if (!JAVA_9_OR_LATER) {
			assertThat(invocation.getErrorOutput(), equalTo(""));
		}
		invocation = this.cli.invoke("jar", jar.getAbsolutePath(), "bad.groovy");
		invocation.await();
		if (!JAVA_9_OR_LATER) {
			assertEquals(invocation.getErrorOutput(), 0,
					invocation.getErrorOutput().length());
		}
		assertTrue(jar.exists());

		Process process = new JavaExecutable()
				.processBuilder("-jar", jar.getAbsolutePath()).start();
		invocation = new Invocation(process);
		invocation.await();

		if (!JAVA_9_OR_LATER) {
			assertThat(invocation.getErrorOutput(), equalTo(""));
		}
	}

	@Test
	public void jarCreation() throws Exception {
		File jar = new File("target/test-app.jar");
		Invocation invocation = this.cli.invoke("jar", jar.getAbsolutePath(),
				"jar.groovy");
		invocation.await();
		if (!JAVA_9_OR_LATER) {
			assertEquals(invocation.getErrorOutput(), 0,
					invocation.getErrorOutput().length());
		}
		assertTrue(jar.exists());

		Process process = new JavaExecutable()
				.processBuilder("-jar", jar.getAbsolutePath()).start();
		invocation = new Invocation(process);
		invocation.await();

		if (!JAVA_9_OR_LATER) {
			assertThat(invocation.getErrorOutput(), equalTo(""));
		}
		assertThat(invocation.getStandardOutput(), containsString("Hello World!"));
		assertThat(invocation.getStandardOutput(),
				containsString("/BOOT-INF/classes!/public/public.txt"));
		assertThat(invocation.getStandardOutput(),
				containsString("/BOOT-INF/classes!/resources/resource.txt"));
		assertThat(invocation.getStandardOutput(),
				containsString("/BOOT-INF/classes!/static/static.txt"));
		assertThat(invocation.getStandardOutput(),
				containsString("/BOOT-INF/classes!/templates/template.txt"));
		assertThat(invocation.getStandardOutput(),
				containsString("/BOOT-INF/classes!/root.properties"));
		assertThat(invocation.getStandardOutput(), containsString("Goodbye Mama"));
	}

	@Test
	public void jarCreationWithIncludes() throws Exception {
		File jar = new File("target/test-app.jar");
		Invocation invocation = this.cli.invoke("jar", jar.getAbsolutePath(), "--include",
				"-public/**,-resources/**", "jar.groovy");
		invocation.await();
		if (!JAVA_9_OR_LATER) {
			assertEquals(invocation.getErrorOutput(), 0,
					invocation.getErrorOutput().length());
		}
		assertTrue(jar.exists());

		Process process = new JavaExecutable()
				.processBuilder("-jar", jar.getAbsolutePath()).start();
		invocation = new Invocation(process);
		invocation.await();

		if (!JAVA_9_OR_LATER) {
			assertThat(invocation.getErrorOutput(), equalTo(""));
		}
		assertThat(invocation.getStandardOutput(), containsString("Hello World!"));
		assertThat(invocation.getStandardOutput(),
				not(containsString("/public/public.txt")));
		assertThat(invocation.getStandardOutput(),
				not(containsString("/resources/resource.txt")));
		assertThat(invocation.getStandardOutput(), containsString("/static/static.txt"));
		assertThat(invocation.getStandardOutput(),
				containsString("/templates/template.txt"));
		assertThat(invocation.getStandardOutput(), containsString("Goodbye Mama"));
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
