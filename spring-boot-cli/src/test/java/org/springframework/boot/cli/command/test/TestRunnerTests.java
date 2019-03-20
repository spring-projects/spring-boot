/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.cli.command.test;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TestRunner}.
 *
 * @author Andy Wilkinson
 */
public class TestRunnerTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void exceptionMessageWhenSourcesContainsNoClasses() throws Exception {
		TestRunnerConfiguration configuration = mock(TestRunnerConfiguration.class);
		given(configuration.getClasspath()).willReturn(new String[0]);
		this.thrown.expect(RuntimeException.class);
		this.thrown.expectMessage(equalTo("No classes found in '[foo, bar]'"));
		new TestRunner(configuration, new String[] { "foo", "bar" }).compileAndRunTests();
	}

}
