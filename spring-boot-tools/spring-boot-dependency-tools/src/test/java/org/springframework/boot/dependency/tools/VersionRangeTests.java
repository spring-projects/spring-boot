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

package org.springframework.boot.dependency.tools;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link VersionRange}.
 *
 * @author Stephane Nicoll
 */
public class VersionRangeTests {
	
	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Test
	public void matchSimpleRange() {
		assertThat("1.2.0.RC3", match("[1.2.0.RC1,1.2.0.RC5]"));
	}

	@Test
	public void matchSimpleRangeBefore() {
		assertThat("1.1.9.RC3", not(match("[1.2.0.RC1,1.2.0.RC5]")));
	}

	@Test
	public void matchSimpleRangeAfter() {
		assertThat("1.2.0.RC6", not(match("[1.2.0.RC1,1.2.0.RC5]")));
	}

	@Test
	public void matchInclusiveLowerRange() {
		assertThat("1.2.0.RC1", match("[1.2.0.RC1,1.2.0.RC5]"));
	}

	@Test
	public void matchInclusiveHigherRange() {
		assertThat("1.2.0.RC5", match("[1.2.0.RC1,1.2.0.RC5]"));
	}

	@Test
	public void matchExclusiveLowerRange() {
		assertThat("1.2.0.RC1", not(match("(1.2.0.RC1,1.2.0.RC5)")));
	}

	@Test
	public void matchExclusiveHigherRange() {
		assertThat("1.2.0.RC5", not(match("[1.2.0.RC1,1.2.0.RC5)")));
	}

	@Test
	public void matchUnboundedRangeEqual() {
		assertThat("1.2.0.RELEASE", match("1.2.0.RELEASE"));
	}

	@Test
	public void matchUnboundedRangeAfter() {
		assertThat("2.2.0.RELEASE", match("1.2.0.RELEASE"));
	}

	@Test
	public void matchUnboundedRangeBefore() {
		assertThat("1.1.9.RELEASE", not(match("1.2.0.RELEASE")));
	}

	@Test
	public void invalidRange() {
		thrown.expect(InvalidVersionException.class);
		VersionRange.parse("foo-bar");
	}

	@Test
	public void rangeWithSpaces() {
		assertThat("1.2.0.RC3", match("[   1.2.0.RC1 ,  1.2.0.RC5]"));
	}

	private static VersionRangeMatcher match(String range) {
		return new VersionRangeMatcher(range);
	}
	

	static class VersionRangeMatcher extends BaseMatcher<String> {

		private final VersionRange range;

		VersionRangeMatcher(String text) {
			this.range = VersionRange.parse(text);
		}

		@Override
		public boolean matches(Object item) {
			return item instanceof String && this.range.match(Version.parse((String) item));
		}

		@Override
		public void describeTo(Description description) {
			description.appendText(range.toString());
		}
	}

}
