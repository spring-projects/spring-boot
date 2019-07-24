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

import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.boot.cli.command.run.RunCommand;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link RunCommand}.
 *
 * @author Andy Wilkinson
 */
public class RunCommandIntegrationTests {

	@Rule
	public CliTester cli = new CliTester("src/it/resources/run-command/");

	private Properties systemProperties = new Properties();

	@Before
	public void captureSystemProperties() {
		this.systemProperties.putAll(System.getProperties());
	}

	@After
	public void restoreSystemProperties() {
		System.setProperties(this.systemProperties);
	}

	@Test
	public void bannerAndLoggingIsOutputByDefault() throws Exception {
		String output = this.cli.run("quiet.groovy");
		assertThat(output).contains(" :: Spring Boot ::");
		assertThat(output).contains("Starting application");
		assertThat(output).contains("Ssshh");
	}

	@Test
	public void quietModeSuppressesAllCliOutput() throws Exception {
		this.cli.run("quiet.groovy");
		String output = this.cli.run("quiet.groovy", "-q");
		assertThat(output).isEqualTo("Ssshh");
	}

}
