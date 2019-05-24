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

import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration tests to exercise and reproduce specific issues.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
@ExtendWith(OutputCaptureExtension.class)
class ReproIntegrationTests {

	@RegisterExtension
	private CliTester cli;

	ReproIntegrationTests(CapturedOutput capturedOutput) {
		this.cli = new CliTester("src/test/resources/repro-samples/", capturedOutput);
	}

	@Test
	void grabAntBuilder() throws Exception {
		this.cli.run("grab-ant-builder.groovy");
		assertThat(this.cli.getHttpOutput()).contains("{\"message\":\"Hello World\"}");
	}

	// Security depends on old versions of Spring so if the dependencies aren't pinned
	// this will fail
	@Test
	void securityDependencies() throws Exception {
		assertThat(this.cli.run("secure.groovy")).contains("Hello World");
	}

	@Test
	void dataJpaDependencies() throws Exception {
		assertThat(this.cli.run("data-jpa.groovy")).contains("Hello World");
	}

	@Test
	void jarFileExtensionNeeded() throws Exception {
		assertThatExceptionOfType(ExecutionException.class)
				.isThrownBy(() -> this.cli.jar("secure.groovy", "data-jpa.groovy"))
				.withMessageContaining("is not a JAR file");
	}

}
