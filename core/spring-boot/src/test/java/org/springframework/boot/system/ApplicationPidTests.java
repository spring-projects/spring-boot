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

package org.springframework.boot.system;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.contentOf;

/**
 * Tests for {@link ApplicationPid}.
 *
 * @author Phillip Webb
 */
class ApplicationPidTests {

	@TempDir
	@SuppressWarnings("NullAway.Init")
	File tempDir;

	@Test
	void toStringWithPid() {
		assertThat(new ApplicationPid(123L)).hasToString("123");
	}

	@Test
	void toStringWithoutPid() {
		assertThat(new ApplicationPid(null)).hasToString("???");
	}

	@Test
	void throwIllegalStateWritingMissingPid() {
		ApplicationPid pid = new ApplicationPid(null);
		assertThatIllegalStateException().isThrownBy(() -> pid.write(new File(this.tempDir, "pid")))
			.withMessageContaining("No PID available");
	}

	@Test
	void writePid() throws Exception {
		ApplicationPid pid = new ApplicationPid(123L);
		File file = new File(this.tempDir, "pid");
		pid.write(file);
		assertThat(contentOf(file)).isEqualTo("123");
	}

	@Test
	void writeNewPid() throws Exception {
		// gh-10784
		ApplicationPid pid = new ApplicationPid(123L);
		File file = new File(this.tempDir, "pid");
		file.delete();
		pid.write(file);
		assertThat(contentOf(file)).isEqualTo("123");
	}

	@Test
	void overwriteExistingPid() throws Exception {
		File file = new File(this.tempDir, "pid");
		new ApplicationPid(123L).write(file);
		assertThat(contentOf(file)).isEqualTo("123");
		new ApplicationPid(456L).write(file);
		assertThat(contentOf(file)).isEqualTo("456");
	}

	@Test
	@DisabledOnOs(OS.WINDOWS)
	void whenSymlinkToNonExistentTargetExistsAtPidFileLocationWriteThrows() throws IOException {
		File link = new File(this.tempDir, "pid");
		File target = new File(this.tempDir, "target");
		Files.createSymbolicLink(link.toPath(), target.toPath());
		ApplicationPid pid = new ApplicationPid(123L);
		assertThatIOException().isThrownBy(() -> pid.write(link));
		assertThat(Files.isSymbolicLink(link.toPath())).isTrue();
		assertThat(target).doesNotExist();
	}

	@Test
	@DisabledOnOs(OS.WINDOWS)
	void whenSymlinkToTargetExistsAtPidFileLocationWriteThrows() throws IOException {
		File link = new File(this.tempDir, "pid");
		Path target = new File(this.tempDir, "target").toPath();
		Files.write(target, "target".getBytes(), StandardOpenOption.CREATE_NEW);
		Files.createSymbolicLink(link.toPath(), target);
		ApplicationPid pid = new ApplicationPid(123L);
		assertThatIOException().isThrownBy(() -> pid.write(link));
		assertThat(Files.isSymbolicLink(link.toPath())).isTrue();
		assertThat(target).hasContent("target");
	}

	@Test
	void toLong() {
		ApplicationPid pid = new ApplicationPid(123L);
		assertThat(pid.toLong()).isEqualTo(123L);
	}

	@Test
	void toLongWhenNotAvailable() {
		ApplicationPid pid = new ApplicationPid(null);
		assertThat(pid.toLong()).isNull();
	}

	@Test
	void isAvailableWhenAvailable() {
		ApplicationPid pid = new ApplicationPid(123L);
		assertThat(pid.isAvailable()).isTrue();
	}

	@Test
	void isAvailableWhenNotAvailable() {
		ApplicationPid pid = new ApplicationPid(null);
		assertThat(pid.isAvailable()).isFalse();
	}

	@Test
	void getPidFromJvm() {
		assertThat(new ApplicationPid().toString()).isNotEmpty();
	}

}
