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

package org.springframework.boot.test.json;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link BasicJsonTester}.
 *
 * @author Phillip Webb
 */
class BasicJsonTesterTests {

	private static final String JSON = "{\"spring\":[\"boot\",\"framework\"]}";

	private BasicJsonTester json = new BasicJsonTester(getClass());

	@Test
	void createWhenResourceLoadClassIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new BasicJsonTester(null))
				.withMessageContaining("ResourceLoadClass must not be null");
	}

	@Test
	void fromJsonStringShouldReturnJsonContent() {
		assertThat(this.json.from(JSON)).isEqualToJson("source.json");
	}

	@Test
	void fromResourceStringShouldReturnJsonContent() {
		assertThat(this.json.from("source.json")).isEqualToJson(JSON);
	}

	@Test
	void fromResourceStringWithClassShouldReturnJsonContent() {
		assertThat(this.json.from("source.json", getClass())).isEqualToJson(JSON);
	}

	@Test
	void fromByteArrayShouldReturnJsonContent() {
		assertThat(this.json.from(JSON.getBytes())).isEqualToJson("source.json");
	}

	@Test
	void fromFileShouldReturnJsonContent(@TempDir Path temp) throws Exception {
		File file = new File(temp.toFile(), "file.json");
		FileCopyUtils.copy(JSON.getBytes(), file);
		assertThat(this.json.from(file)).isEqualToJson("source.json");
	}

	@Test
	void fromInputStreamShouldReturnJsonContent() {
		InputStream inputStream = new ByteArrayInputStream(JSON.getBytes());
		assertThat(this.json.from(inputStream)).isEqualToJson("source.json");
	}

	@Test
	void fromResourceShouldReturnJsonContent() {
		Resource resource = new ByteArrayResource(JSON.getBytes());
		assertThat(this.json.from(resource)).isEqualToJson("source.json");
	}

}
