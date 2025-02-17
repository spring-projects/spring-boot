/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.maven;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ClasspathBuilder}.
 *
 * @author Dmytro Nosan
 */
class ClasspathBuilderTests {

	@Test
	void buildWithEmptyClassPath() {
		assertThat(ClasspathBuilder.build()).isEmpty();
	}

	@Test
	void buildWithSingleClassPathURL(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("test.jar");
		assertThat(ClasspathBuilder.build(file.toUri().toURL())).isEqualTo(file.toString());
	}

	@Test
	@DisabledOnOs(OS.WINDOWS)
	void buildWithMultipleClassPathURLs(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("test.jar");
		Path file1 = tempDir.resolve("test1.jar");
		assertThat(ClasspathBuilder.build(file.toUri().toURL(), file1.toUri().toURL()))
			.isEqualTo(file + File.pathSeparator + file1);
	}

	@Test
	@EnabledOnOs(OS.WINDOWS)
	void buildWithMultipleClassPathURLsOnWindows(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("test.jar");
		Path file1 = tempDir.resolve("test1.jar");
		String classpath = ClasspathBuilder.build(file.toUri().toURL(), file1.toUri().toURL());
		assertThat(classpath).startsWith("@");
		assertThat(Paths.get(classpath.substring(1)))
			.hasContent("\"" + (file + File.pathSeparator + file1).replace("\\", "\\\\") + "\"");
	}

}
