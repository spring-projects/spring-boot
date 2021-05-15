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
 * Integration tests for code in directories.
 *
 * @author Dave Syer
 */
@ExtendWith(OutputCaptureExtension.class)
class DirectorySourcesIntegrationTests {

	@RegisterExtension
	CliTester cli;

	DirectorySourcesIntegrationTests(CapturedOutput output) {
		this.cli = new CliTester("src/test/resources/dir-sample/", output);
	}

	@Test
	void runDirectory() throws Exception {
		assertThat(this.cli.run("code")).contains("Hello World");
	}

	@Test
	void runDirectoryRecursive() throws Exception {
		assertThat(this.cli.run("")).contains("Hello World");
	}

	@Test
	void runPathPattern() throws Exception {
		assertThat(this.cli.run("**/*.groovy")).contains("Hello World");
	}

}
