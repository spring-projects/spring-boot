/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.bind;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultPropertyNamePatternsMatcher}.
 *
 * @author Phillip Webb
 */
public class DefaultPropertyNamePatternsMatcherTests {

	private static final char[] DELIMITERS = { '.', '_' };

	@Test
	public void namesShorter() {
		assertThat(new DefaultPropertyNamePatternsMatcher(DELIMITERS, "aaaa", "bbbb")
				.matches("zzzzz")).isFalse();

	}

	@Test
	public void namesExactMatch() {
		assertThat(
				new DefaultPropertyNamePatternsMatcher(DELIMITERS, "aaaa", "bbbb", "cccc")
						.matches("bbbb")).isTrue();
	}

	@Test
	public void namesLonger() {
		assertThat(new DefaultPropertyNamePatternsMatcher(DELIMITERS, "aaaaa", "bbbbb",
				"ccccc").matches("bbbb")).isFalse();
	}

	@Test
	public void nameWithDot() throws Exception {
		assertThat(
				new DefaultPropertyNamePatternsMatcher(DELIMITERS, "aaaa", "bbbb", "cccc")
						.matches("bbbb.anything")).isTrue();
	}

	@Test
	public void nameWithUnderscore() throws Exception {
		assertThat(
				new DefaultPropertyNamePatternsMatcher(DELIMITERS, "aaaa", "bbbb", "cccc")
						.matches("bbbb_anything")).isTrue();
	}

	@Test
	public void namesMatchWithDifferentLengths() throws Exception {
		assertThat(
				new DefaultPropertyNamePatternsMatcher(DELIMITERS, "aaa", "bbbb", "ccccc")
						.matches("bbbb")).isTrue();
	}

	@Test
	public void withSquareBrackets() throws Exception {
		char[] delimiters = "._[".toCharArray();
		PropertyNamePatternsMatcher matcher = new DefaultPropertyNamePatternsMatcher(
				delimiters, "aaa", "bbbb", "ccccc");
		assertThat(matcher.matches("bbbb")).isTrue();
		assertThat(matcher.matches("bbbb[4]")).isTrue();
		assertThat(matcher.matches("bbb[4]")).isFalse();
	}

}
