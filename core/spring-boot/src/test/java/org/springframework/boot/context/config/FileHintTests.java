/*
 * Copyright 2012-present the original author or authors.
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
