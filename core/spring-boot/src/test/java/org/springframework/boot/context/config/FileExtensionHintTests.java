/*
 * Copyright 2012-present the original author or authors.
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
