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

package org.springframework.boot.system;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.contentOf;

/**
 * Tests for {@link ApplicationPid}.
 *
 * @author Phillip Webb
 */
public class ApplicationPidTests {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void toStringWithPid() {
		assertThat(new ApplicationPid("123").toString()).isEqualTo("123");
	}

	@Test
	public void toStringWithoutPid() {
		assertThat(new ApplicationPid(null).toString()).isEqualTo("???");
	}

	@Test
	public void throwIllegalStateWritingMissingPid() {
		ApplicationPid pid = new ApplicationPid(null);
		assertThatIllegalStateException()
				.isThrownBy(() -> pid.write(this.temporaryFolder.newFile()))
				.withMessageContaining("No PID available");
	}

	@Test
	public void writePid() throws Exception {
		ApplicationPid pid = new ApplicationPid("123");
		File file = this.temporaryFolder.newFile();
		pid.write(file);
		assertThat(contentOf(file)).isEqualTo("123");
	}

	@Test
	public void writeNewPid() throws Exception {
		// gh-10784
		ApplicationPid pid = new ApplicationPid("123");
		File file = this.temporaryFolder.newFile();
		file.delete();
		pid.write(file);
		assertThat(contentOf(file)).isEqualTo("123");
	}

	@Test
	public void getPidFromJvm() {
		assertThat(new ApplicationPid().toString()).isNotEmpty();
	}

}
