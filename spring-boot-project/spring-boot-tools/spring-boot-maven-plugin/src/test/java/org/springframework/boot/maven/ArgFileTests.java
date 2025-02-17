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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ArgFile}.
 *
 * @author Moritz Halbritter
 * @author Dmytro Nosan
 */
class ArgFileTests {

	@Test
	void argFileEscapesContent() throws IOException {
		ArgFile file = ArgFile.create("some \\ content");
		assertThat(Paths.get(file.toString())).content(StandardCharsets.UTF_8).isEqualTo("\"some \\\\ content\"");
	}

}
