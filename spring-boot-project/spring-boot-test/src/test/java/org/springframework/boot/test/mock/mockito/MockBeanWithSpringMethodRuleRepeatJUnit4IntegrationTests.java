/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.test.mock.mockito;

import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.test.annotation.Repeat;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MockBean} and {@link Repeat}.
 *
 * @author Andy Wilkinson
 * @see <a href="https://github.com/spring-projects/spring-boot/issues/27693">gh-27693</a>
 */
public class MockBeanWithSpringMethodRuleRepeatJUnit4IntegrationTests {

	@Rule
	public final SpringMethodRule springMethodRule = new SpringMethodRule();

	@MockBean
	private FirstService first;

	private static int invocations;

	@AfterClass
	public static void afterClass() {
		assertThat(invocations).isEqualTo(2);
	}

	@Test
	@Repeat(2)
	public void repeatedTest() {
		invocations++;
	}

	interface FirstService {

		String greeting();

	}

}
