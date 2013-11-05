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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;

import org.codehaus.plexus.util.FileUtils;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests to exercise the samples.
 * 
 * @author Dave Syer
 * @author Greg Turnquist
 * @author Roy Clarkson
 * @author Phillip Webb
 */
public class SampleIntegrationTests {

	@Rule
	public CliTester cli = new CliTester();

	@BeforeClass
	public static void cleanGrapes() throws Exception {
		GrapesCleaner.cleanIfNecessary();
	}

	@Test
	public void appSample() throws Exception {
		String output = this.cli.run("samples/app.groovy");
		assertTrue("Wrong output: " + output, output.contains("Hello World"));
	}

	@Test
	public void templateSample() throws Exception {
		String output = this.cli.run("samples/template.groovy");
		assertTrue("Wrong output: " + output, output.contains("Hello World!"));
	}

	@Test
	public void jobSample() throws Exception {
		String output = this.cli.run("samples/job.groovy", "foo=bar");
		assertTrue("Wrong output: " + output,
				output.contains("completed with the following parameters"));
	}

	@Test
	public void reactorSample() throws Exception {
		String output = this.cli.run("samples/reactor.groovy", "Phil");
		int count = 0;
		while (!output.contains("Hello Phil") && count++ < 5) {
			Thread.sleep(200);
			output = this.cli.getOutput();
		}
		assertTrue("Wrong output: " + output, output.contains("Hello Phil"));
	}

	@Test
	public void jobWebSample() throws Exception {
		String output = this.cli.run("samples/job.groovy", "samples/web.groovy",
				"foo=bar");
		assertTrue("Wrong output: " + output,
				output.contains("completed with the following parameters"));
		String result = readEntirely("http://localhost:8080");
		assertEquals("World!", result);
	}

	@Test
	public void webSample() throws Exception {
		this.cli.run("samples/web.groovy");
		String result = readEntirely("http://localhost:8080");
		assertEquals("World!", result);
	}

	@Test
	public void uiSample() throws Exception {
		this.cli.run("samples/ui.groovy", "--classpath=.:src/test/resources");
		String result = readEntirely("http://localhost:8080");
		assertTrue("Wrong output: " + result, result.contains("Hello World"));
		result = readEntirely("http://localhost:8080/css/bootstrap.min.css");
		assertTrue("Wrong output: " + result, result.contains("container"));
	}

	@Test
	public void actuatorSample() throws Exception {
		this.cli.run("samples/actuator.groovy");
		String result = readEntirely("http://localhost:8080");
		assertEquals("{\"message\":\"Hello World!\"}", result);
	}

	@Test
	public void httpSample() throws Exception {
		String output = this.cli.run("samples/http.groovy");
		assertTrue("Wrong output: " + output, output.contains("Hello World"));
	}

	@Test
	public void integrationSample() throws Exception {
		String output = this.cli.run("samples/integration.groovy");
		assertTrue("Wrong output: " + output, output.contains("Hello, World"));
	}

	@Test
	public void xmlSample() throws Exception {
		String output = this.cli.run("samples/runner.xml", "samples/runner.groovy");
		assertTrue("Wrong output: " + output, output.contains("Hello World"));
	}

	@Test
	public void txSample() throws Exception {
		String output = this.cli.run("samples/tx.groovy");
		assertTrue("Wrong output: " + output, output.contains("Foo count="));
	}

	@Test
	public void jmsSample() throws Exception {
		String output = this.cli.run("samples/jms.groovy");
		assertTrue("Wrong output: " + output,
				output.contains("Received Greetings from Spring Boot via ActiveMQ"));
		FileUtils.deleteDirectory(new File("activemq-data"));// cleanup ActiveMQ cruft
	}

	@Test
	@Ignore
	// this test requires RabbitMQ to be run, so disable it be default
	public void rabbitSample() throws Exception {
		String output = this.cli.run("samples/rabbit.groovy");
		assertTrue("Wrong output: " + output,
				output.contains("Received Greetings from Spring Boot via RabbitMQ"));
	}

	@Test
	public void deviceSample() throws Exception {
		this.cli.run("samples/device.groovy");
		String result = readEntirely("http://localhost:8080");
		assertEquals("Hello Normal Device!", result);
	}

	private static String readEntirely(String uri) {
		try {
			InputStream stream = URI.create(uri).toURL().openStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
			String line;
			StringBuilder result = new StringBuilder();
			while ((line = reader.readLine()) != null) {
				result.append(line);
			}
			return result.toString();
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

}
