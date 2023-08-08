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

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link DockerComposeFile}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class DockerComposeFileTests {

	@TempDir
	File temp;

	@Test
	void hashCodeAndEquals() throws Exception {
		File f1 = new File(this.temp, "compose.yml");
		File f2 = new File(this.temp, "docker-compose.yml");
		FileCopyUtils.copy(new byte[0], f1);
		FileCopyUtils.copy(new byte[0], f2);
		DockerComposeFile c1 = DockerComposeFile.of(f1);
		DockerComposeFile c2 = DockerComposeFile.of(f1);
		DockerComposeFile c3 = DockerComposeFile.find(f1.getParentFile());
		DockerComposeFile c4 = DockerComposeFile.of(f2);
		assertThat(c1.hashCode()).isEqualTo(c2.hashCode()).isEqualTo(c3.hashCode());
		assertThat(c1).isEqualTo(c1).isEqualTo(c2).isEqualTo(c3).isNotEqualTo(c4);
	}

	@Test
	void toStringReturnsFileName() throws Exception {
		DockerComposeFile composeFile = createComposeFile("compose.yml");
		assertThat(composeFile.toString()).endsWith(File.separator + "compose.yml");
	}

	@Test
	void findFindsSingleFile() throws Exception {
		File file = new File(this.temp, "docker-compose.yml");
		FileCopyUtils.copy(new byte[0], file);
		DockerComposeFile composeFile = DockerComposeFile.find(file.getParentFile());
		assertThat(composeFile.toString()).endsWith(File.separator + "docker-compose.yml");
	}

	@Test
	void findWhenMultipleFilesPicksBest() throws Exception {
		File f1 = new File(this.temp, "docker-compose.yml");
		FileCopyUtils.copy(new byte[0], f1);
		File f2 = new File(this.temp, "compose.yml");
		FileCopyUtils.copy(new byte[0], f2);
		DockerComposeFile composeFile = DockerComposeFile.find(f1.getParentFile());
		assertThat(composeFile.toString()).endsWith(File.separator + "compose.yml");
	}

	@Test
	void findWhenNoComposeFilesReturnsNull() throws Exception {
		File file = new File(this.temp, "not-a-compose.yml");
		FileCopyUtils.copy(new byte[0], file);
		DockerComposeFile composeFile = DockerComposeFile.find(file.getParentFile());
		assertThat(composeFile).isNull();
	}

	@Test
	void findWhenWorkingDirectoryDoesNotExistReturnsNull() {
		File directory = new File(this.temp, "missing");
		DockerComposeFile composeFile = DockerComposeFile.find(directory);
		assertThat(composeFile).isNull();
	}

	@Test
	void findWhenWorkingDirectoryIsNotDirectoryThrowsException() throws Exception {
		File file = new File(this.temp, "iamafile");
		FileCopyUtils.copy(new byte[0], file);
		assertThatIllegalArgumentException().isThrownBy(() -> DockerComposeFile.find(file))
			.withMessageEndingWith("is not a directory");
	}

	@Test
	void ofReturnsDockerComposeFile() throws Exception {
		File file = new File(this.temp, "anyfile.yml");
		FileCopyUtils.copy(new byte[0], file);
		DockerComposeFile composeFile = DockerComposeFile.of(file);
		assertThat(composeFile).isNotNull();
		assertThat(composeFile).hasToString(file.getCanonicalPath());
	}

	@Test
	void ofWhenFileIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> DockerComposeFile.of(null))
			.withMessage("File must not be null");
	}

	@Test
	void ofWhenFileDoesNotExistThrowsException() {
		File file = new File(this.temp, "missing");
		assertThatIllegalArgumentException().isThrownBy(() -> DockerComposeFile.of(file))
			.withMessageEndingWith("does not exist");
	}

	@Test
	void ofWhenFileIsNotFileThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> DockerComposeFile.of(this.temp))
			.withMessageEndingWith("is not a file");
	}

	private DockerComposeFile createComposeFile(String name) throws IOException {
		File file = new File(this.temp, name);
		FileCopyUtils.copy(new byte[0], file);
		return DockerComposeFile.of(file);
	}

}
