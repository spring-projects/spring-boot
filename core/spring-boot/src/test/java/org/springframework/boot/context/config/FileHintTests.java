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

package org.springframework.boot.context.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FileHint}.
 *
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
class FileHintTests {

	@Test
	void shouldParseImplicitExtension() {
		String value = "foo[.bar]";
		FileHint hint = FileHint.from(value);
		assertThat(hint.isPresent()).isTrue();
		assertThat(hint.getExtension()).isEqualTo(".bar");
		assertThat(FileHint.removeFrom(value)).isEqualTo("foo");
	}

	@Test
	void shouldParseExplicitExtension() {
		String value = "foo[extension=.bar]";
		FileHint hint = FileHint.from(value);
		assertThat(hint.isPresent()).isTrue();
		assertThat(hint.getExtension()).isEqualTo(".bar");
		assertThat(FileHint.removeFrom(value)).isEqualTo("foo");
	}

	@Test
	void shouldParseNestedBrackets() {
		String value = "foo[encoding=[utf-8]]";
		FileHint hint = FileHint.from(value);
		assertThat(hint.isPresent()).isTrue();
		assertThat(hint.getEncoding()).isEqualTo("[utf-8]");
		assertThat(FileHint.removeFrom(value)).isEqualTo("foo");
	}

	@Test
	void shouldParseEncoding() {
		String value = "foo[encoding=utf-8]";
		FileHint hint = FileHint.from(value);
		assertThat(hint.isPresent()).isTrue();
		assertThat(hint.getEncoding()).isEqualTo("utf-8");
		assertThat(FileHint.removeFrom(value)).isEqualTo("foo");
	}

	@Test
	void shouldParseAll() {
		String value = "foo[extension=.bar][encoding=utf-8]";
		FileHint hint = FileHint.from(value);
		assertThat(hint.isPresent()).isTrue();
		assertThat(hint.getExtension()).isEqualTo(".bar");
		assertThat(hint.getEncoding()).isEqualTo("utf-8");
		assertThat(FileHint.removeFrom(value)).isEqualTo("foo");
	}

	@Test
	void isPresentWhenHasNoHint() {
		assertThat(FileHint.from("foo").isPresent()).isFalse();
		assertThat(FileHint.from("foo[bar]").isPresent()).isFalse();
		assertThat(FileHint.from("foo[.b[ar]").isPresent()).isFalse();
	}

	@Test
	void getExtensionOrElseWhenHasHint() {
		assertThat(FileHint.from("foo[.bar]").getExtensionOrElse(".txt")).isEqualTo(".bar");
	}

	@Test
	void getExtensionOrElseWhenHasNoHint() {
		assertThat(FileHint.from("foo").getExtensionOrElse(".txt")).isEqualTo(".txt");
	}

	@Test
	void removeFromWhenHasNoHint() {
		assertThat(FileHint.removeFrom("foo")).isEqualTo("foo");
	}

}
