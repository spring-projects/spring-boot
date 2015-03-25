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
import java.net.URI;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
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
	public CliTester cli = new CliTester("samples/");

	@Test
	public void appSample() throws Exception {
		String output = this.cli.run("app.groovy");
		URI scriptUri = new File("samples/app.groovy").toURI();
		assertTrue("Wrong output: " + output,
				output.contains("Hello World! From " + scriptUri));
	}

	@Test
	public void beansSample() throws Exception {
		this.cli.run("beans.groovy");
		String output = this.cli.getHttpOutput();
		assertTrue("Wrong output: " + output, output.contains("Hello World!"));
	}

	@Test
	public void templateSample() throws Exception {
		String output = this.cli.run("template.groovy");
		assertTrue("Wrong output: " + output, output.contains("Hello World!"));
	}

	@Test
	@Ignore("Due to the removal of ParameterizedRowMapper, Spring Batch is incompatible with Spring Framework 4.2")
	public void jobSample() throws Exception {
		String output = this.cli.run("job.groovy", "foo=bar");
		assertTrue("Wrong output: " + output,
				output.contains("completed with the following parameters"));
	}

	@Test
	public void reactorSample() throws Exception {
		String output = this.cli.run("reactor.groovy", "Phil");
		int count = 0;
		while (!output.contains("Hello Phil") && count++ < 5) {
			Thread.sleep(200);
			output = this.cli.getOutput();
		}
		assertTrue("Wrong output: " + output, output.contains("Hello Phil"));
	}

	@Test
	@Ignore("Spring Batch is incompatible with Spring Framework 4.2")
	public void jobWebSample() throws Exception {
		String output = this.cli.run("job.groovy", "web.groovy", "foo=bar");
		assertTrue("Wrong output: " + output,
				output.contains("completed with the following parameters"));
		String result = this.cli.getHttpOutput();
		assertEquals("World!", result);
	}

	@Test
	public void webSample() throws Exception {
		this.cli.run("web.groovy");
		assertEquals("World!", this.cli.getHttpOutput());
	}

	@Test
	public void uiSample() throws Exception {
		this.cli.run("ui.groovy", "--classpath=.:src/test/resources");
		String result = this.cli.getHttpOutput();
		assertTrue("Wrong output: " + result, result.contains("Hello World"));
		result = this.cli.getHttpOutput("/css/bootstrap.min.css");
		assertTrue("Wrong output: " + result, result.contains("container"));
	}

	@Test
	public void actuatorSample() throws Exception {
		this.cli.run("actuator.groovy");
		assertEquals("{\"message\":\"Hello World!\"}", this.cli.getHttpOutput());
	}

	@Test
	public void httpSample() throws Exception {
		String output = this.cli.run("http.groovy");
		assertTrue("Wrong output: " + output, output.contains("Hello World"));
	}

	@Test
	public void integrationSample() throws Exception {
		String output = this.cli.run("integration.groovy");
		assertTrue("Wrong output: " + output, output.contains("Hello, World"));
	}

	@Test
	public void xmlSample() throws Exception {
		String output = this.cli.run("runner.xml", "runner.groovy");
		assertTrue("Wrong output: " + output, output.contains("Hello World"));
	}

	@Test
	public void txSample() throws Exception {
		String output = this.cli.run("tx.groovy");
		assertTrue("Wrong output: " + output, output.contains("Foo count="));
	}

	@Test
	public void jmsSample() throws Exception {
		String output = this.cli.run("jms.groovy");
		assertTrue("Wrong output: " + output,
				output.contains("Received Greetings from Spring Boot via HornetQ"));
	}

	@Test
	@Ignore("Requires RabbitMQ to be run, so disable it be default")
	public void rabbitSample() throws Exception {
		String output = this.cli.run("rabbit.groovy");
		assertTrue("Wrong output: " + output,
				output.contains("Received Greetings from Spring Boot via RabbitMQ"));
	}

	@Test
	public void deviceSample() throws Exception {
		this.cli.run("device.groovy");
		assertEquals("Hello Normal Device!", this.cli.getHttpOutput());
	}

	@Test
	public void caching() throws Exception {
		this.cli.run("caching.groovy");
		assertThat(this.cli.getOutput(), containsString("Hello World"));
	}

}
