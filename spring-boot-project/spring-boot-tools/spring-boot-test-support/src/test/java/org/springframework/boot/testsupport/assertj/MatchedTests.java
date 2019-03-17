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

package org.springframework.boot.testsupport.assertj;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.Matchers.startsWith;

/**
 * Tests for {@link Matched}.
 *
 * @author Phillip Webb
 */
public class MatchedTests {

	@Test
	public void byMatcherMatches() {
		assertThat("1234").is(Matched.by(startsWith("12")));
	}

	@Test
	public void byMatcherDoesNotMatch() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat("1234").is(Matched.by(startsWith("23"))))
				.withMessageContaining("a string starting with \"23\"");
	}

	@Test
	public void whenMatcherMatches() {
		assertThat("1234").is(Matched.when(startsWith("12")));
	}

	@Test
	public void whenMatcherDoesNotMatch() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat("1234").is(Matched.when(startsWith("23"))))
				.withMessageContaining("a string starting with \"23\"");
	}

}
