/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.loader.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Tests for @{link NativeImageArgFile}.
 *
 * @author Moritz Halbritter
 */
class NativeImageArgFileTests {

	@Test
	void writeIfNecessaryWhenHasExcludesWritesLines() {
		NativeImageArgFile argFile = new NativeImageArgFile(List.of("path/to/dependency-1.jar", "dependency-2.jar"));
		List<String> lines = new ArrayList<>();
		argFile.writeIfNecessary(lines::addAll);
		assertThat(lines).containsExactly("--exclude-config", "\\Qdependency-1.jar\\E", "^/META-INF/native-image/.*",
				"--exclude-config", "\\Qdependency-2.jar\\E", "^/META-INF/native-image/.*");
	}

	@Test
	void writeIfNecessaryWhenHasNothingDoesNotCallConsumer() {
		NativeImageArgFile argFile = new NativeImageArgFile(Collections.emptyList());
		argFile.writeIfNecessary((lines) -> fail("Should not be called"));
	}

}
