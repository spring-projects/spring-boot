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

package org.springframework.boot.context.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FileExtensionHint}.
 *
 * @author Phillip Webb
 */
class FileExtensionHintTests {

	@Test
	void isPresentWhenHasHint() {
		assertThat(FileExtensionHint.from("foo[.bar]").isPresent()).isTrue();
	}

	@Test
	void isPresentWhenHasNoHint() {
		assertThat(FileExtensionHint.from("foo").isPresent()).isFalse();
		assertThat(FileExtensionHint.from("foo[bar]").isPresent()).isFalse();
		assertThat(FileExtensionHint.from("foo[.b[ar]").isPresent()).isFalse();
	}

	@Test
	void orElseWhenHasHint() {
		assertThat(FileExtensionHint.from("foo[.bar]").orElse(".txt")).isEqualTo(".bar");
	}

	@Test
	void orElseWhenHasNoHint() {
		assertThat(FileExtensionHint.from("foo").orElse(".txt")).isEqualTo(".txt");
	}

	@Test
	void toStringWhenHasHintReturnsDotExtension() {
		assertThat(FileExtensionHint.from("foo[.bar]")).hasToString(".bar");
	}

	@Test
	void toStringWhenHasNoHintReturnsEmpty() {
		assertThat(FileExtensionHint.from("foo")).hasToString("");
	}

	@Test
	void removeFromWhenHasHint() {
		assertThat(FileExtensionHint.removeFrom("foo[.bar]")).isEqualTo("foo");
	}

	@Test
	void removeFromWhenHasNoHint() {
		assertThat(FileExtensionHint.removeFrom("foo[bar]")).isEqualTo("foo[bar]");
	}

}
