/*
 * Copyright 2012-2013 the original author or authors.
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
package org.springframework.bootstrap.cli;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.ivy.util.FileUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Dave Syer
 * 
 */
public class SampleIntegrationTests {

	private RunCommand command;

	private PrintStream savedOutput;
	private ByteArrayOutputStream output;

	@Before
	public void init() {
		this.savedOutput = System.out;
		this.output = new ByteArrayOutputStream();
		System.setOut(new PrintStream(this.output));
	}

	@After
	public void clear() {
		System.setOut(this.savedOutput);
	}

	private String getOutput() {
		return this.output.toString();
	}

	private void start(final String sample) throws Exception {
		Future<RunCommand> future = Executors.newSingleThreadExecutor().submit(
				new Callable<RunCommand>() {
					@Override
					public RunCommand call() throws Exception {
						RunCommand command = new RunCommand();
						command.run(sample);
						return command;
					}
				});
		this.command = future.get(10, TimeUnit.SECONDS);
	}

	@After
	public void stop() {
		if (this.command != null) {
			this.command.stop();
		}
	}

	@BeforeClass
	public static void clean() {
		// SpringBootstrapCli.main("clean");
		// System.setProperty("ivy.message.logger.level", "3");
	}

	@Test
	public void appSample() throws Exception {
		start("samples/app.groovy");
		String output = getOutput();
		assertTrue("Wrong output: " + output, output.contains("Hello World"));
	}

	@Test
	public void jobSample() throws Exception {
		start("samples/job.groovy");
		String output = getOutput();
		assertTrue("Wrong output: " + output,
				output.contains("completed with the following parameters"));
	}

	@Test
	public void webSample() throws Exception {
		start("samples/web.groovy");
		String result = FileUtil.readEntirely(new URL("http://localhost:8080")
				.openStream());
		assertEquals("World!", result);
	}

	@Test
	public void serviceSample() throws Exception {
		start("samples/service.groovy");
		String result = FileUtil.readEntirely(new URL("http://localhost:8080")
				.openStream());
		assertEquals("{\"message\":\"Hello World!\"}", result);
	}

	@Test
	public void integrationSample() throws Exception {
		start("samples/integration.groovy");
		String output = getOutput();
		assertTrue("Wrong output: " + output, output.contains("Hello, World"));
	}

}
