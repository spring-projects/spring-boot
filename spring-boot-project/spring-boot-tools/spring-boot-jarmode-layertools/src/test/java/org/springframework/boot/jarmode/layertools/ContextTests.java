/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.jarmode.layertools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link Context}.
 *
 * @author Phillip Webb
 */
class ContextTests {

	@TempDir
	File temp;

	@Test
	void createWhenSourceIsNullThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> new Context(null, this.temp))
				.withMessage("Unable to find source JAR");
	}

	@Test
	void createWhenSourceIsFolderThrowsException() {
		File folder = new File(this.temp, "test");
		folder.mkdir();
		assertThatIllegalStateException().isThrownBy(() -> new Context(folder, this.temp))
				.withMessage("Unable to find source JAR");
	}

	@Test
	void createWhenSourceIsNotJarThrowsException() throws Exception {
		File zip = new File(this.temp, "test.zip");
		Files.createFile(zip.toPath());
		assertThatIllegalStateException().isThrownBy(() -> new Context(zip, this.temp))
				.withMessage("Unable to find source JAR");
	}

	@Test
	void getJarFileReturnsJar() throws Exception {
		File jar = new File(this.temp, "test.jar");
		Files.createFile(jar.toPath());
		Context context = new Context(jar, this.temp);
		assertThat(context.getJarFile()).isEqualTo(jar);
	}

	@Test
	void getWorkingDirectoryReturnsWorkingDir() throws IOException {
		File jar = new File(this.temp, "test.jar");
		Files.createFile(jar.toPath());
		Context context = new Context(jar, this.temp);
		assertThat(context.getWorkingDir()).isEqualTo(this.temp);

	}

	@Test
	void getRelativePathReturnsRelativePath() throws Exception {
		File target = new File(this.temp, "target");
		target.mkdir();
		File jar = new File(target, "test.jar");
		Files.createFile(jar.toPath());
		Context context = new Context(jar, this.temp);
		assertThat(context.getRelativeJarDir()).isEqualTo("target");
	}

	@Test
	void getRelativePathWhenWorkingDirReturnsNull() throws Exception {
		File jar = new File(this.temp, "test.jar");
		Files.createFile(jar.toPath());
		Context context = new Context(jar, this.temp);
		assertThat(context.getRelativeJarDir()).isNull();
	}

	@Test
	void getRelativePathWhenCannotBeDeducedReturnsNull() throws Exception {
		File folder1 = new File(this.temp, "folder1");
		folder1.mkdir();
		File folder2 = new File(this.temp, "folder1");
		folder2.mkdir();
		File jar = new File(folder1, "test.jar");
		Files.createFile(jar.toPath());
		Context context = new Context(jar, folder2);
		assertThat(context.getRelativeJarDir()).isNull();
	}

}
