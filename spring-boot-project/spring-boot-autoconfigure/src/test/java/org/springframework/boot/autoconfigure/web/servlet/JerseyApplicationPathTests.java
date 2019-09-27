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
package org.springframework.boot.autoconfigure.web.servlet;

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
