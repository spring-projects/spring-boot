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

package org.springframework.boot.web.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import org.springframework.boot.web.server.AbstractConfigurableWebServerFactory.TempDirs;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AbstractConfigurableWebServerFactory}.
 *
 * @author Moritz Halbritter
 */
class AbstractConfigurableWebServerFactoryTests {

	@Test
	void shouldCleanUpTempDirs() throws IOException {
		TempDirs tempDirs = new TempDirs(1234);
		Path tempDir1 = tempDirs.createTempDir(getClass().getSimpleName());
		Path tempDir2 = tempDirs.createTempDir(getClass().getSimpleName());
		assertThat(tempDir1).isNotEqualTo(tempDir2);
		assertThat(tempDir1).exists();
		assertThat(tempDir2).exists();
		Files.writeString(tempDir1.resolve("test.txt"), "test");
		tempDirs.cleanup();
		assertThat(tempDir1).doesNotExist();
		assertThat(tempDir2).doesNotExist();
	}

}
