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

package org.springframework.boot.devtools.autoconfigure;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link TriggerFileFilter}.
 *
 * @author Phillip Webb
 */
class TriggerFileFilterTests {

	@TempDir
	File tempDir;

	@Test
	void nameMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> new TriggerFileFilter(null))
				.withMessageContaining("Name must not be null");
	}

	@Test
	void acceptNameMatch() throws Exception {
		File file = new File(this.tempDir, "thefile.txt");
		file.createNewFile();
		assertThat(new TriggerFileFilter("thefile.txt").accept(file)).isTrue();
	}

	@Test
	void doesNotAcceptNameMismatch() throws Exception {
		File file = new File(this.tempDir, "notthefile.txt");
		file.createNewFile();
		assertThat(new TriggerFileFilter("thefile.txt").accept(file)).isFalse();
	}

	@Test
	void testName() throws Exception {
		File file = new File(this.tempDir, ".triggerfile");
		file.createNewFile();
		assertThat(new TriggerFileFilter(".triggerfile").accept(file)).isTrue();
	}

}
