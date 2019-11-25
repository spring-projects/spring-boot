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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for CLI Classloader issues.
 *
 * @author Phillip Webb
 */
@ExtendWith(OutputCaptureExtension.class)
class ClassLoaderIntegrationTests {

	@RegisterExtension
	CliTester cli;

	ClassLoaderIntegrationTests(CapturedOutput output) {
		this.cli = new CliTester("src/test/resources/", output);
	}

	@Test
	void runWithIsolatedClassLoader() throws Exception {
		// CLI classes or dependencies should not be exposed to the app
		String output = this.cli.run("classloader-test-app.groovy", SpringCli.class.getName());
		assertThat(output).contains("HasClasses-false-true-false");
	}

}
