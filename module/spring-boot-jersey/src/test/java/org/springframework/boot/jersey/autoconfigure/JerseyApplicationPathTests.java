/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jersey.autoconfigure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JerseyApplicationPath}.
 *
 * @author Madhura Bhave
 */
class JerseyApplicationPathTests {

	@Test
	void getRelativePathReturnsRelativePath() {
		assertThat(((JerseyApplicationPath) () -> "spring").getRelativePath("boot")).isEqualTo("spring/boot");
		assertThat(((JerseyApplicationPath) () -> "spring/").getRelativePath("boot")).isEqualTo("spring/boot");
		assertThat(((JerseyApplicationPath) () -> "spring").getRelativePath("/boot")).isEqualTo("spring/boot");
		assertThat(((JerseyApplicationPath) () -> "spring/*").getRelativePath("/boot")).isEqualTo("spring/boot");
	}

	@Test
	void getPrefixWhenHasSimplePathReturnPath() {
		assertThat(((JerseyApplicationPath) () -> "spring").getPrefix()).isEqualTo("spring");
	}

	@Test
	void getPrefixWhenHasPatternRemovesPattern() {
		assertThat(((JerseyApplicationPath) () -> "spring/*.do").getPrefix()).isEqualTo("spring");
	}

	@Test
	void getPrefixWhenPathEndsWithSlashRemovesSlash() {
		assertThat(((JerseyApplicationPath) () -> "spring/").getPrefix()).isEqualTo("spring");
	}

	@Test
	void getUrlMappingWhenPathIsEmptyReturnsSlash() {
		assertThat(((JerseyApplicationPath) () -> "").getUrlMapping()).isEqualTo("/*");
	}

	@Test
	void getUrlMappingWhenPathIsSlashReturnsSlash() {
		assertThat(((JerseyApplicationPath) () -> "/").getUrlMapping()).isEqualTo("/*");
	}

	@Test
	void getUrlMappingWhenPathContainsStarReturnsPath() {
		assertThat(((JerseyApplicationPath) () -> "/spring/*.do").getUrlMapping()).isEqualTo("/spring/*.do");
	}

	@Test
	void getUrlMappingWhenHasPathNotEndingSlashReturnsSlashStarPattern() {
		assertThat(((JerseyApplicationPath) () -> "/spring/boot").getUrlMapping()).isEqualTo("/spring/boot/*");
	}

	@Test
	void getUrlMappingWhenHasPathDoesNotStartWithSlashPrependsSlash() {
		assertThat(((JerseyApplicationPath) () -> "spring/boot").getUrlMapping()).isEqualTo("/spring/boot/*");
	}

	@Test
	void getUrlMappingWhenHasPathEndingWithSlashReturnsSlashStarPattern() {
		assertThat(((JerseyApplicationPath) () -> "/spring/boot/").getUrlMapping()).isEqualTo("/spring/boot/*");
	}

}
