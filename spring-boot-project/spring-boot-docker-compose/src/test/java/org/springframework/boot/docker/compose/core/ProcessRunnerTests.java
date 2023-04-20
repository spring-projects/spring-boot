/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.docker.compose.core;

import org.junit.jupiter.api.Test;

import org.springframework.boot.testsupport.process.DisabledIfProcessUnavailable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link ProcessRunner}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
@DisabledIfProcessUnavailable("docker")
class ProcessRunnerTests {

	private ProcessRunner processRunner = new ProcessRunner();

	@Test
	void run() {
		String out = this.processRunner.run("docker", "--version");
		assertThat(out).isNotEmpty();
	}

	@Test
	void runWhenHasOutputConsumer() {
		StringBuilder output = new StringBuilder();
		this.processRunner.run(output::append, "docker", "--version");
		assertThat(output.toString()).isNotEmpty();
	}

	@Test
	void runWhenProcessDoesNotStart() {
		assertThatExceptionOfType(ProcessStartException.class)
			.isThrownBy(() -> this.processRunner.run("iverymuchdontexist", "--version"));
	}

	@Test
	void runWhenProcessReturnsNonZeroExitCode() {
		assertThatExceptionOfType(ProcessExitException.class)
			.isThrownBy(() -> this.processRunner.run("docker", "-thisdoesntwork"))
			.satisfies((ex) -> {
				assertThat(ex.getExitCode()).isGreaterThan(0);
				assertThat(ex.getStdOut()).isEmpty();
				assertThat(ex.getStdErr()).isNotEmpty();
			});
	}

}
