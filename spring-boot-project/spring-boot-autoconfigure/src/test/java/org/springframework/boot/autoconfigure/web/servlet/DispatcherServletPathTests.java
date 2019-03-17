/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.web.servlet;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DispatcherServletPath}.
 *
 * @author Phillip Webb
 */
public class DispatcherServletPathTests {

	@Test
	public void getRelativePathReturnsRelativePath() {
		assertThat(((DispatcherServletPath) () -> "spring").getRelativePath("boot"))
				.isEqualTo("spring/boot");
		assertThat(((DispatcherServletPath) () -> "spring/").getRelativePath("boot"))
				.isEqualTo("spring/boot");
		assertThat(((DispatcherServletPath) () -> "spring").getRelativePath("/boot"))
				.isEqualTo("spring/boot");
	}

	@Test
	public void getPrefixWhenHasSimplePathReturnPath() {
		assertThat(((DispatcherServletPath) () -> "spring").getPrefix())
				.isEqualTo("spring");
	}

	@Test
	public void getPrefixWhenHasPatternRemovesPattern() {
		assertThat(((DispatcherServletPath) () -> "spring/*.do").getPrefix())
				.isEqualTo("spring");
	}

	@Test
	public void getPathWhenPathEndsWithSlashRemovesSlash() {
		assertThat(((DispatcherServletPath) () -> "spring/").getPrefix())
				.isEqualTo("spring");
	}

	@Test
	public void getServletUrlMappingWhenPathIsEmptyReturnsSlash() {
		assertThat(((DispatcherServletPath) () -> "").getServletUrlMapping())
				.isEqualTo("/");
	}

	@Test
	public void getServletUrlMappingWhenPathIsSlashReturnsSlash() {
		assertThat(((DispatcherServletPath) () -> "/").getServletUrlMapping())
				.isEqualTo("/");
	}

	@Test
	public void getServletUrlMappingWhenPathContainsStarReturnsPath() {
		assertThat(((DispatcherServletPath) () -> "spring/*.do").getServletUrlMapping())
				.isEqualTo("spring/*.do");
	}

	@Test
	public void getServletUrlMappingWhenHasPathNotEndingSlashReturnsSlashStarPattern() {
		assertThat(((DispatcherServletPath) () -> "spring/boot").getServletUrlMapping())
				.isEqualTo("spring/boot/*");
	}

	@Test
	public void getServletUrlMappingWhenHasPathEndingWithSlashReturnsSlashStarPattern() {
		assertThat(((DispatcherServletPath) () -> "spring/boot/").getServletUrlMapping())
				.isEqualTo("spring/boot/*");
	}

}
