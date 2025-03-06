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
 * Tests for {@link DispatcherServletPath}.
 *
 * @author Phillip Webb
 */
class DispatcherServletPathTests {

	@Test
	void getRelativePathReturnsRelativePath() {
		assertThat(((DispatcherServletPath) () -> "spring").getRelativePath("boot")).isEqualTo("spring/boot");
		assertThat(((DispatcherServletPath) () -> "spring/").getRelativePath("boot")).isEqualTo("spring/boot");
		assertThat(((DispatcherServletPath) () -> "spring").getRelativePath("/boot")).isEqualTo("spring/boot");
	}

	@Test
	void getPrefixWhenHasSimplePathReturnPath() {
		assertThat(((DispatcherServletPath) () -> "spring").getPrefix()).isEqualTo("spring");
	}

	@Test
	void getPrefixWhenHasPatternRemovesPattern() {
		assertThat(((DispatcherServletPath) () -> "spring/*.do").getPrefix()).isEqualTo("spring");
	}

	@Test
	void getPathWhenPathEndsWithSlashRemovesSlash() {
		assertThat(((DispatcherServletPath) () -> "spring/").getPrefix()).isEqualTo("spring");
	}

	@Test
	void getServletUrlMappingWhenPathIsEmptyReturnsSlash() {
		assertThat(((DispatcherServletPath) () -> "").getServletUrlMapping()).isEqualTo("/");
	}

	@Test
	void getServletUrlMappingWhenPathIsSlashReturnsSlash() {
		assertThat(((DispatcherServletPath) () -> "/").getServletUrlMapping()).isEqualTo("/");
	}

	@Test
	void getServletUrlMappingWhenPathContainsStarReturnsPath() {
		assertThat(((DispatcherServletPath) () -> "spring/*.do").getServletUrlMapping()).isEqualTo("spring/*.do");
	}

	@Test
	void getServletUrlMappingWhenHasPathNotEndingSlashReturnsSlashStarPattern() {
		assertThat(((DispatcherServletPath) () -> "spring/boot").getServletUrlMapping()).isEqualTo("spring/boot/*");
	}

	@Test
	void getServletUrlMappingWhenHasPathEndingWithSlashReturnsSlashStarPattern() {
		assertThat(((DispatcherServletPath) () -> "spring/boot/").getServletUrlMapping()).isEqualTo("spring/boot/*");
	}

}
