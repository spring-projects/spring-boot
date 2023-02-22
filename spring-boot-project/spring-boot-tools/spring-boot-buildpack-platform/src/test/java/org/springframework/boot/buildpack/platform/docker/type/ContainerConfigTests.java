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

package org.springframework.boot.buildpack.platform.docker.type;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import org.springframework.boot.buildpack.platform.json.AbstractJsonTests;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ContainerConfig}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @author Jeroen Meijer
 */
class ContainerConfigTests extends AbstractJsonTests {

	@Test
	void ofWhenImageReferenceIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> ContainerConfig.of(null, (update) -> {
		})).withMessage("ImageReference must not be null");
	}

	@Test
	void ofWhenUpdateIsNullThrowsException() {
		ImageReference imageReference = ImageReference.of("ubuntu:bionic");
		assertThatIllegalArgumentException().isThrownBy(() -> ContainerConfig.of(imageReference, null))
			.withMessage("Update must not be null");
	}

	@Test
	void writeToWritesJson() throws Exception {
		ImageReference imageReference = ImageReference.of("ubuntu:bionic");
		ContainerConfig containerConfig = ContainerConfig.of(imageReference, (update) -> {
			update.withUser("root");
			update.withCommand("ls", "-l");
			update.withArgs("-h");
			update.withLabel("spring", "boot");
			update.withBinding(Binding.from("bind-source", "bind-dest"));
			update.withEnv("name1", "value1");
			update.withEnv("name2", "value2");
			update.withNetworkMode("test");
			update.withSecurityOption("option=value");
		});
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		containerConfig.writeTo(outputStream);
		String actualJson = outputStream.toString(StandardCharsets.UTF_8);
		String expectedJson = StreamUtils.copyToString(getContent("container-config.json"), StandardCharsets.UTF_8);
		JSONAssert.assertEquals(expectedJson, actualJson, true);
	}

}
