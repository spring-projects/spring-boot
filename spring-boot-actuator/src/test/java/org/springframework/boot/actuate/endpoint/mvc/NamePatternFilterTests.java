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
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link NamePatternFilter}.
 *
 * @author Phillip Webb
 */
public class NamePatternFilterTests {

	@Test
	public void nonRegex() throws Exception {
		MockNamePatternFilter filter = new MockNamePatternFilter();
		assertThat(filter.getResults("not.a.regex"), equalTo((Object) "not.a.regex"));
		assertThat(filter.isGetNamesCalled(), equalTo(false));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void regex() throws Exception {
		MockNamePatternFilter filter = new MockNamePatternFilter();
		Map<String, Object> results = (Map<String, Object>) filter.getResults("fo.*");
		assertThat(results.get("foo"), equalTo((Object) "foo"));
		assertThat(results.get("fool"), equalTo((Object) "fool"));
		assertThat(filter.isGetNamesCalled(), equalTo(true));

	}

	private static class MockNamePatternFilter extends NamePatternFilter<Object> {

		public MockNamePatternFilter() {
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
