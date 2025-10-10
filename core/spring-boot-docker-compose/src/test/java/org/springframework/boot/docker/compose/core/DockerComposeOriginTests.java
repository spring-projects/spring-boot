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

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DockerComposeOrigin}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class DockerComposeOriginTests {

	@TempDir
	@SuppressWarnings("NullAway.Init")
	File temp;

	@Test
	void hasToString() throws Exception {
		DockerComposeFile composeFile = createTempComposeFile();
		DockerComposeOrigin origin = new DockerComposeOrigin(composeFile, "service-1");
		assertThat(origin.toString()).startsWith("Docker compose service 'service-1' defined in ")
			.endsWith("compose.yaml");
	}

	@Test
	void hasToStringWithMultipleFiles() throws IOException {
		File file1 = createTempFile("1.yaml");
		File file2 = createTempFile("2.yaml");
		DockerComposeOrigin origin = new DockerComposeOrigin(DockerComposeFile.of(List.of(file1, file2)), "service-1");
		assertThat(origin.toString())
			.startsWith("Docker compose service 'service-1' defined in %s, %s".formatted(file1, file2));
	}

	@Test
	void equalsAndHashcode() throws Exception {
		DockerComposeFile composeFile = createTempComposeFile();
		DockerComposeOrigin origin1 = new DockerComposeOrigin(composeFile, "service-1");
		DockerComposeOrigin origin2 = new DockerComposeOrigin(composeFile, "service-1");
		DockerComposeOrigin origin3 = new DockerComposeOrigin(composeFile, "service-3");
		assertThat(origin1).isEqualTo(origin1);
		assertThat(origin1).isEqualTo(origin2);
		assertThat(origin1).hasSameHashCodeAs(origin2);
		assertThat(origin2).isEqualTo(origin1);
		assertThat(origin1).isNotEqualTo(origin3);
		assertThat(origin2).isNotEqualTo(origin3);
		assertThat(origin3).isNotEqualTo(origin1);
		assertThat(origin3).isNotEqualTo(origin2);
	}

	private DockerComposeFile createTempComposeFile() throws IOException {
		return DockerComposeFile.of(createTempFile("compose.yaml"));
	}

	private File createTempFile(String filename) throws IOException {
		File file = new File(this.temp, filename);
		FileCopyUtils.copy(new byte[0], file);
		return file.getCanonicalFile();
	}

}
