/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.mvc;

import java.util.Map;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link NamePatternFilter}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class NamePatternFilterTests {

	@Test
	public void nonRegex() throws Exception {
		MockNamePatternFilter filter = new MockNamePatternFilter();
		assertThat(filter.getResults("not.a.regex"),
				hasEntry("not.a.regex", (Object) "not.a.regex"));
		assertThat(filter.isGetNamesCalled(), equalTo(false));
	}

	@Test
	public void regexRepetitionZeroOrMore() {
		MockNamePatternFilter filter = new MockNamePatternFilter();
		Map<String, Object> results = filter.getResults("fo.*");
		assertThat(results.get("foo"), equalTo((Object) "foo"));
		assertThat(results.get("fool"), equalTo((Object) "fool"));
		assertThat(filter.isGetNamesCalled(), equalTo(true));
	}

	@Test
	public void regexRepetitionOneOrMore() {
		MockNamePatternFilter filter = new MockNamePatternFilter();
		Map<String, Object> results = filter.getResults("fo.+");
		assertThat(results.get("foo"), equalTo((Object) "foo"));
		assertThat(results.get("fool"), equalTo((Object) "fool"));
		assertThat(filter.isGetNamesCalled(), equalTo(true));
	}

	@Test
	public void regexEndAnchor() {
		MockNamePatternFilter filter = new MockNamePatternFilter();
		Map<String, Object> results = filter.getResults("foo$");
		assertThat(results.get("foo"), equalTo((Object) "foo"));
		assertThat(results.get("fool"), is(nullValue()));
		assertThat(filter.isGetNamesCalled(), equalTo(true));
	}

	@Test
	public void regexStartAnchor() {
		MockNamePatternFilter filter = new MockNamePatternFilter();
		Map<String, Object> results = filter.getResults("^foo");
		assertThat(results.get("foo"), equalTo((Object) "foo"));
		assertThat(results.get("fool"), is(nullValue()));
		assertThat(filter.isGetNamesCalled(), equalTo(true));
	}

	@Test
	public void regexCharacterClass() {
		MockNamePatternFilter filter = new MockNamePatternFilter();
		Map<String, Object> results = filter.getResults("fo[a-z]l");
		assertThat(results.get("foo"), is(nullValue()));
		assertThat(results.get("fool"), equalTo((Object) "fool"));
		assertThat(filter.isGetNamesCalled(), equalTo(true));
	}

	private static class MockNamePatternFilter extends NamePatternFilter<Object> {

		MockNamePatternFilter() {
			super(null);
		}

		private boolean getNamesCalled;

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
