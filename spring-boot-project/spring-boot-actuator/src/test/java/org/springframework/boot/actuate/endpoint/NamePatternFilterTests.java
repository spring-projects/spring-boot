/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.endpoint;

import java.util.Map;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NamePatternFilter}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Dylian Bego
 */
public class NamePatternFilterTests {

	@Test
	public void nonRegex() throws Exception {
		MockNamePatternFilter filter = new MockNamePatternFilter();
		assertThat(filter.getResults("not.a.regex")).containsEntry("not.a.regex",
				"not.a.regex");
		assertThat(filter.isGetNamesCalled()).isFalse();
	}

	@Test
	public void nonRegexThatContainsRegexPart() throws Exception {
		MockNamePatternFilter filter = new MockNamePatternFilter();
		assertThat(filter.getResults("*")).containsEntry("*", "*");
		assertThat(filter.isGetNamesCalled()).isFalse();
	}

	@Test
	public void regexRepetitionZeroOrMore() {
		MockNamePatternFilter filter = new MockNamePatternFilter();
		Map<String, Object> results = filter.getResults("fo.*");
		assertThat(results.get("foo")).isEqualTo("foo");
		assertThat(results.get("fool")).isEqualTo("fool");
		assertThat(filter.isGetNamesCalled()).isTrue();
	}

	@Test
	public void regexRepetitionOneOrMore() {
		MockNamePatternFilter filter = new MockNamePatternFilter();
		Map<String, Object> results = filter.getResults("fo.+");
		assertThat(results.get("foo")).isEqualTo("foo");
		assertThat(results.get("fool")).isEqualTo("fool");
		assertThat(filter.isGetNamesCalled()).isTrue();
	}

	@Test
	public void regexEndAnchor() {
		MockNamePatternFilter filter = new MockNamePatternFilter();
		Map<String, Object> results = filter.getResults("foo$");
		assertThat(results.get("foo")).isEqualTo("foo");
		assertThat(results.get("fool")).isNull();
		assertThat(filter.isGetNamesCalled()).isTrue();
	}

	@Test
	public void regexStartAnchor() {
		MockNamePatternFilter filter = new MockNamePatternFilter();
		Map<String, Object> results = filter.getResults("^foo");
		assertThat(results.get("foo")).isEqualTo("foo");
		assertThat(results.get("fool")).isNull();
		assertThat(filter.isGetNamesCalled()).isTrue();
	}

	@Test
	public void regexCharacterClass() {
		MockNamePatternFilter filter = new MockNamePatternFilter();
		Map<String, Object> results = filter.getResults("fo[a-z]l");
		assertThat(results.get("foo")).isNull();
		assertThat(results.get("fool")).isEqualTo("fool");
		assertThat(filter.isGetNamesCalled()).isTrue();
	}

	private static class MockNamePatternFilter extends NamePatternFilter<Object> {

		MockNamePatternFilter() {
			super(null);
		}

		private boolean getNamesCalled;

		@Override
		protected Object getOptionalValue(Object source, String name) {
			return name;
		}

		@Override
		protected Object getValue(Object source, String name) {
			return name;
		}

		@Override
		protected void getNames(Object source, NameCallback callback) {
			this.getNamesCalled = true;
			callback.addName("foo");
			callback.addName("fool");
			callback.addName("fume");
		}

		public boolean isGetNamesCalled() {
			return this.getNamesCalled;
		}

	}

}
