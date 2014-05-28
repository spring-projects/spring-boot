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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.boot.cli.command.test.TestCommand;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

/**
 * Integration tests to exercise the CLI's test command.
 * 
 * @author Greg Turnquist
 * @author Phillip Webb
 */
public class TestCommandIntegrationTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public CliTester cli = new CliTester("test-samples/");

	@Before
	public void setup() throws Exception {
		System.setProperty("disableSpringSnapshotRepos", "false");
	}

	@After
	public void teardown() {
		System.clearProperty("disableSpringSnapshotRepos");
	}

	@Test
	public void noTests() throws Throwable {
		String output = this.cli.test("book.groovy");
		assertThat(output, containsString("No tests found"));
	}

	@Test
	public void empty() throws Exception {
		String output = this.cli.test("empty.groovy");
		assertThat(output, containsString("No tests found"));
	}

	@Test
	public void noFile() throws Exception {
		TestCommand command = new TestCommand();
		this.thrown.expect(RuntimeException.class);
		this.thrown.expectMessage("Can't find nothing.groovy");
		command.run("nothing.groovy");
	}

	@Test
	public void appAndTestsInOneFile() throws Exception {
		String output = this.cli.test("book_and_tests.groovy");
		assertThat(output, containsString("OK (1 test)"));
	}

	@Test
	public void appInOneFileTestsInAnotherFile() throws Exception {
		String output = this.cli.test("book.groovy", "test.groovy");
		assertThat(output, containsString("OK (1 test)"));
	}

	@Test
	public void integrationTest() throws Exception {
		String output = this.cli.test("integration.groovy");
		assertThat(output, containsString("OK (1 test)"));
	}

	@Test
	public void spockTester() throws Exception {
		String output = this.cli.test("spock.groovy");
		assertThat(output, containsString("OK (1 test)"));
	}

	@Test
	public void spockAndJunitTester() throws Exception {
		String output = this.cli.test("spock.groovy", "book_and_tests.groovy");
		assertThat(output, containsString("OK (2 tests)"));
	}

	@Test
	public void verifyFailures() throws Exception {
		String output = this.cli.test("failures.groovy");
		assertThat(output, containsString("Tests run: 5,  Failures: 3"));
	}
}
