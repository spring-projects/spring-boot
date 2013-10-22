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

package org.springframework.boot.cli;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.OutputCapture;
import org.springframework.boot.cli.command.CleanCommand;
import org.springframework.boot.cli.command.RunCommand;
import org.springframework.boot.cli.util.FileUtils;
import org.springframework.boot.cli.util.IoUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests to exercise the samples.
 * 
 * @author Dave Syer
 * @author Greg Turnquist
 * @author Roy Clarkson
 */
public class SampleIntegrationTests {

	@BeforeClass
	public static void cleanGrapes() throws Exception {
		GrapesCleaner.cleanIfNecessary();
		// System.setProperty("ivy.message.logger.level", "3");
	}

	@Rule
	public OutputCapture outputCapture = new OutputCapture();

	private RunCommand command;

	private void start(final String... sample) throws Exception {
		Future<RunCommand> future = Executors.newSingleThreadExecutor().submit(
				new Callable<RunCommand>() {
					@Override
					public RunCommand call() throws Exception {
						RunCommand command = new RunCommand();
						command.run(sample);
						return command;
					}
				});
		this.command = future.get(6, TimeUnit.MINUTES);
	}

	@Before
	public void setup() throws Exception {
		System.setProperty("disableSpringSnapshotRepos", "true");
		new CleanCommand().run("org.springframework");
	}

	@After
	public void teardown() {
		System.clearProperty("disableSpringSnapshotRepos");
	}

	@After
	public void stop() {
		if (this.command != null) {
			this.command.stop();
		}
	}

	@Test
	public void appSample() throws Exception {
		start("samples/app.groovy");
		String output = this.outputCapture.getOutputAndRelease();
		assertTrue("Wrong output: " + output, output.contains("Hello World"));
	}

	@Test
	public void templateSample() throws Exception {
		start("samples/template.groovy");
		String output = this.outputCapture.getOutputAndRelease();
		assertTrue("Wrong output: " + output, output.contains("Hello World!"));
	}

	@Test
	public void jobSample() throws Exception {
		start("samples/job.groovy", "foo=bar");
		String output = this.outputCapture.getOutputAndRelease();
		System.out.println(output);
		assertTrue("Wrong output: " + output,
				output.contains("completed with the following parameters"));
	}

	@Test
	public void reactorSample() throws Exception {
		start("samples/reactor.groovy", "Phil");
		String output = this.outputCapture.getOutputAndRelease();
		int count = 0;
		while (!output.contains("Hello Phil") && count++ < 5) {
			Thread.sleep(200);
			output = this.outputCapture.getOutputAndRelease();
		}
		assertTrue("Wrong output: " + output, output.contains("Hello Phil"));
	}

	@Test
	public void jobWebSample() throws Exception {
		start("samples/job.groovy", "samples/web.groovy", "foo=bar");
		String output = this.outputCapture.getOutputAndRelease();
		assertTrue("Wrong output: " + output,
				output.contains("completed with the following parameters"));
		String result = IoUtils.readEntirely("http://localhost:8080");
		assertEquals("World!", result);
	}

	@Test
	public void webSample() throws Exception {
		start("samples/web.groovy");
		String result = IoUtils.readEntirely("http://localhost:8080");
		assertEquals("World!", result);
	}

	@Test
	public void uiSample() throws Exception {
		start("samples/ui.groovy", "--classpath=.:src/test/resources");
		String result = IoUtils.readEntirely("http://localhost:8080");
		assertTrue("Wrong output: " + result, result.contains("Hello World"));
		result = IoUtils.readEntirely("http://localhost:8080/css/bootstrap.min.css");
		assertTrue("Wrong output: " + result, result.contains("container"));
	}

	@Test
	public void actuatorSample() throws Exception {
		start("samples/actuator.groovy");
		String result = IoUtils.readEntirely("http://localhost:8080");
		assertEquals("{\"message\":\"Hello World!\"}", result);
	}

	@Test
	public void httpSample() throws Exception {
		start("samples/http.groovy");
		String output = this.outputCapture.getOutputAndRelease();
		assertTrue("Wrong output: " + output, output.contains("Hello World"));
	}

	@Test
	public void integrationSample() throws Exception {
		start("samples/integration.groovy");
		String output = this.outputCapture.getOutputAndRelease();
		assertTrue("Wrong output: " + output, output.contains("Hello, World"));
	}

	@Test
	public void xmlSample() throws Exception {
		start("samples/runner.xml", "samples/runner.groovy");
		String output = this.outputCapture.getOutputAndRelease();
		assertTrue("Wrong output: " + output, output.contains("Hello World"));
	}

	@Test
	public void txSample() throws Exception {
		start("samples/tx.groovy");
		String output = this.outputCapture.getOutputAndRelease();
		assertTrue("Wrong output: " + output, output.contains("Foo count="));
	}

	@Test
	public void jmsSample() throws Exception {
		start("samples/jms.groovy");
		String output = this.outputCapture.getOutputAndRelease();
		assertTrue("Wrong output: " + output,
				output.contains("Received Greetings from Spring Boot via ActiveMQ"));
		FileUtils.recursiveDelete(new File("activemq-data")); // cleanup ActiveMQ cruft
	}

	@Test
	@Ignore
	// this test requires RabbitMQ to be run, so disable it be default
	public void rabbitSample() throws Exception {
		start("samples/rabbit.groovy");
		String output = this.outputCapture.getOutputAndRelease();
		assertTrue("Wrong output: " + output,
				output.contains("Received Greetings from Spring Boot via RabbitMQ"));
	}

	@Test
	public void deviceSample() throws Exception {
		start("samples/device.groovy");
		String result = IoUtils.readEntirely("http://localhost:8080");
		assertEquals("Hello Normal Device!", result);
	}

}
