/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.cli;

import java.io.File;

import org.junit.Test;
import org.springframework.boot.cli.command.jar.JarCommand;
import org.springframework.boot.cli.infrastructure.CommandLineInvoker;
import org.springframework.boot.cli.infrastructure.CommandLineInvoker.Invocation;
import org.springframework.boot.cli.util.JavaExecutable;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Integration test for {@link JarCommand}.
 * 
 * @author Andy Wilkinson
 */
public class JarCommandIT {

	private final CommandLineInvoker cli = new CommandLineInvoker(new File(
			"src/it/resources/jar-command"));

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
	public void jarCreation() throws Exception {
		File jar = new File("target/test-app.jar");
		Invocation invocation = this.cli.invoke("jar", jar.getAbsolutePath(),
				"jar.groovy");
		invocation.await();
		assertEquals(invocation.getErrorOutput(), 0, invocation.getErrorOutput().length());
		assertTrue(jar.exists());

		Process process = new JavaExecutable().processBuilder("-jar",
				jar.getAbsolutePath()).start();
		invocation = new Invocation(process);
		invocation.await();

		assertThat(invocation.getErrorOutput(), equalTo(""));
		assertThat(invocation.getStandardOutput(), containsString("Hello World!"));
		assertThat(invocation.getStandardOutput(), containsString("/static/test.txt"));
	}
}
