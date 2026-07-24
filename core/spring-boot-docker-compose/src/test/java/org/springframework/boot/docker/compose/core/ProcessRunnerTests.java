/*
 * Copyright 2012-present the original author or authors.
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.Timeout.ThreadMode;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import org.springframework.boot.testsupport.process.DisabledIfProcessUnavailable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link ProcessRunner}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Sebastien Tardif
 */
class ProcessRunnerTests {

	private final ProcessRunner processRunner = new ProcessRunner();

	@Test
	@DisabledIfProcessUnavailable("docker")
	void run() {
		String out = this.processRunner.run("docker", "--version");
		assertThat(out).isNotEmpty();
	}

	@Test
	@DisabledIfProcessUnavailable("docker")
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
	@DisabledIfProcessUnavailable("docker")
	void runWhenProcessReturnsNonZeroExitCode() {
		assertThatExceptionOfType(ProcessExitException.class)
			.isThrownBy(() -> this.processRunner.run("docker", "-thisdoesntwork"))
			.satisfies((ex) -> {
				assertThat(ex.getExitCode()).isGreaterThan(0);
				assertThat(ex.getStdOut()).isEmpty();
				assertThat(ex.getStdErr()).isNotEmpty();
			});
	}

	@Test
	@DisabledOnOs(OS.WINDOWS)
	@Timeout(value = 5, threadMode = ThreadMode.SEPARATE_THREAD)
	void runWhenOutputConsumerThrowsDoesNotHang() {
		this.processRunner.run((line) -> {
			throw new IllegalStateException("boom");
		}, "echo", "hello");
	}

	@Test
	@DisabledOnOs(OS.WINDOWS)
	@Timeout(value = 10, threadMode = ThreadMode.SEPARATE_THREAD)
	void runWhenInterruptedDestroysChildProcess() throws Exception {
		Path pidFile = Files.createTempFile("process-runner-", ".pid");
		Files.delete(pidFile);
		AtomicReference<Throwable> error = new AtomicReference<>();
		Thread runner = new Thread(() -> {
			try {
				this.processRunner.run("sh", "-c", "echo $$ > '" + pidFile + "'; exec sleep 60");
			}
			catch (Throwable ex) {
				error.set(ex);
			}
		}, "process-runner-interrupt-test");
		runner.start();
		long deadline = System.currentTimeMillis() + 5000;
		while (!Files.exists(pidFile) && System.currentTimeMillis() < deadline) {
			Thread.sleep(50);
		}
		assertThat(pidFile).exists();
		long pid = Long.parseLong(Files.readString(pidFile).trim());
		assertThat(ProcessHandle.of(pid)).isPresent().get().matches(ProcessHandle::isAlive);
		runner.interrupt();
		runner.join(Duration.ofSeconds(5).toMillis());
		assertThat(runner.isAlive()).isFalse();
		assertThat(error.get()).isInstanceOf(IllegalStateException.class);
		assertThat(ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false)).isFalse();
	}

}
