/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.testsupport.runner.classpath;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.isA;

/**
 * Tests for {@link ModifiedClassPathRunner} excluding entries from the class path.
 *
 * @author Andy Wilkinson
 */
@RunWith(ModifiedClassPathRunner.class)
@ClassPathExclusions("hibernate-validator-*.jar")
public class ModifiedClassPathRunnerExclusionsTests {

	private static final String EXCLUDED_RESOURCE = "META-INF/services/"
			+ "javax.validation.spi.ValidationProvider";

	@Test
	public void entriesAreFilteredFromTestClassClassLoader() {
		assertThat(getClass().getClassLoader().getResource(EXCLUDED_RESOURCE)).isNull();
	}

	@Test
	public void entriesAreFilteredFromThreadContextClassLoader() {
		assertThat(Thread.currentThread().getContextClassLoader()
				.getResource(EXCLUDED_RESOURCE)).isNull();
	}

	@Test
	public void testsThatUseHamcrestWorkCorrectly() {
		Matcher<IllegalStateException> matcher = isA(IllegalStateException.class);
		assertThat(matcher.matches(new IllegalStateException())).isTrue();
	}

}
