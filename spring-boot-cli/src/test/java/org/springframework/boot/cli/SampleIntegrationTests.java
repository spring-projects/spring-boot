/*
 * Copyright 2012-2017 the original author or authors.
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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import org.springframework.boot.cli.infrastructure.CommandLineInvoker;
import org.springframework.util.SocketUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests to exercise the samples.
 *
 * @author Dave Syer
 * @author Greg Turnquist
 * @author Roy Clarkson
 * @author Phillip Webb
 * @author Tomek Kopczynski
 */
public class SampleIntegrationTests {

	private static final int PORT = SocketUtils.findAvailableTcpPort();

	private CommandLineInvoker invoker = new CommandLineInvoker();
	private CommandLineInvoker.Invocation invocation = null;

	@After
	public void clean() throws IOException {
		if (this.invocation != null) {
			if (this.invocation.isAlive()) {
				// kill daemon java process
				URI.create(String.format("http://localhost:%d/shutdown", PORT)).toURL().openStream().close(); // don't care about the output
			}
			this.invocation.destroy();
		}
		this.invocation = null;
	}

	private String callEndpoint(String uri) {
		try {
			InputStream stream = URI.create("http://localhost:" + PORT + uri).toURL()
					.openStream();
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

	private void validateScriptExecution(String expectedOutput, String... args) throws InterruptedException, IOException {
		this.invocation = this.invoker.invoke(args);
		assertThat(this.invocation.await()).isEqualTo(0);
		assertThat(this.invocation.getErrorOutput().length()).isEqualTo(0);
		assertThat(this.invocation.getOutput()).contains(expectedOutput);
	}

	private void validateServerExecution(String... args) throws IOException {
		this.invocation = this.invoker.invoke(args);
		assertThat(this.invocation.waitForStarted()).isTrue();
		assertThat(this.invocation.getErrorOutput().length()).isEqualTo(0);
	}

	private void validateOutput(String... expectedOutputs) {
		String output = this.invocation.getOutput();
		for (String expectedOutput : expectedOutputs) {
			assertThat(output).contains(expectedOutput);
		}
	}

	private void validateEndpoint(String uri, String expectedOutput) {
		String output = callEndpoint(uri);
		assertThat(output).contains(expectedOutput);
	}

	private void validateEndpointExactOutput(String uri, String expectedOutput) {
		String output = callEndpoint(uri);
		assertThat(output).isEqualTo(expectedOutput);
	}

	@Test
	public void appSample() throws Exception {
		URI scriptUri = new File("samples/app.groovy").toURI();
		validateScriptExecution(String.format("Hello World! From %s", scriptUri), "run", "samples/app.groovy");
	}

	@Test
	public void retrySample() throws Exception {
		URI scriptUri = new File("samples/retry.groovy").toURI();
		validateScriptExecution(String.format("Hello World! From %s", scriptUri), "run", "samples/retry.groovy");
	}

	@Test
	public void beansSample() throws Exception {
		validateServerExecution("run", "samples/beans.groovy", "src/test/resources/shutdown.groovy", "--", String.format("--server.port=%d", PORT));
		validateEndpoint("/", "Hello World!");
	}

	@Test
	public void templateSample() throws Exception {
		validateScriptExecution("Hello World!", "run", "-cp", "src/test/resources", "samples/template.groovy");
	}

	@Test
	public void jobSample() throws Exception {
		validateScriptExecution("completed with the following parameters", "run", "samples/job.groovy", "foo=bar");
	}

	@Test
	public void oauth2Sample() throws Exception {
		validateServerExecution("run", "samples/oauth2.groovy", "src/test/resources/shutdown.groovy", "src/test/resources/securityConfiguration.groovy", "--", String.format("--server.port=%d", PORT));
		validateOutput("security.oauth2.client.clientId", "security.oauth2.client.secret =");
	}

	@Test
	public void jobWebSample() throws Exception {
		validateServerExecution("run", "samples/job.groovy", "samples/web.groovy", "src/test/resources/shutdown.groovy", "foo=bar", "--", String.format("--server.port=%d", PORT));
		validateOutput("completed with the following parameters");
		validateEndpoint("/", "World!");
	}

	@Test
	public void webSample() throws Exception {
		validateServerExecution("run", "samples/web.groovy", "src/test/resources/shutdown.groovy", "--", String.format("--server.port=%d", PORT));
		validateEndpoint("/", "World!");
	}

	@Test
	public void uiSample() throws Exception {
		validateServerExecution("run", "-cp", "src/test/resources", "samples/ui.groovy", "src/test/resources/shutdown.groovy", "--", String.format("--server.port=%d", PORT));
		validateEndpoint("/", "Hello World");
		validateEndpoint("/css/bootstrap.min.css", "container");
	}

	@Test
	public void actuatorSample() throws Exception {
		validateServerExecution("run", "samples/actuator.groovy", "src/test/resources/shutdown.groovy", "--", String.format("--server.port=%d", PORT));
		validateEndpointExactOutput("/", "{\"message\":\"Hello World!\"}");
	}

	@Test
	public void httpSample() throws Exception {
		validateServerExecution("run", "samples/http.groovy", "src/test/resources/shutdown.groovy", "--", String.format("--server.port=%d", PORT));
		validateOutput("Hello World");
	}

	@Test
	public void integrationSample() throws Exception {
		validateScriptExecution("Hello, World", "run", "samples/integration.groovy");
	}

	@Test
	public void xmlSample() throws Exception {
		validateScriptExecution("Hello World", "run", "samples/runner.xml", "samples/runner.groovy");
	}

	@Test
	public void txSample() throws Exception {
		validateScriptExecution("Foo count=", "run", "-cp", "src/test/resources", "samples/tx.groovy");
	}

	@Test
	public void jmsSample() throws Exception {
		System.setProperty("spring.artemis.embedded.queues", "spring-boot");
		try {
			validateServerExecution("run", "samples/jms.groovy", "src/test/resources/shutdown.groovy", "--", String.format("--server.port=%d", PORT));
			validateOutput("Received Greetings from Spring Boot via Artemis");
		}
		finally {
			System.clearProperty("spring.artemis.embedded.queues");
		}
	}

	@Test
	@Ignore("Requires RabbitMQ to be run, so disable it be default")
	public void rabbitSample() throws Exception {
		validateScriptExecution("Received Greetings from Spring Boot via RabbitMQ", "run", "samples/rabbit.groovy");
	}

	@Test
	public void deviceSample() throws Exception {
		validateServerExecution("run", "samples/device.groovy", "src/test/resources/shutdown.groovy", "--", String.format("--server.port=%d", PORT));
		validateEndpoint("/", "Hello Normal Device!");
	}

	@Test
	public void caching() throws Exception {
		validateScriptExecution("Hello World", "run", "samples/caching.groovy");
	}
}
