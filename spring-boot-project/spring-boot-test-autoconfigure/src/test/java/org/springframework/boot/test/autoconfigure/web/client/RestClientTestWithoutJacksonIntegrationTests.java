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

package org.springframework.boot.test.autoconfigure.web.client;

import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.RunWith;

import org.springframework.boot.testsupport.runner.classpath.ClassPathExclusions;
import org.springframework.boot.testsupport.runner.classpath.ModifiedClassPathRunner;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RestClientTest @RestClientTest} without Jackson.
 *
 * @author Andy Wilkinson
 */
@RunWith(ModifiedClassPathRunner.class)
@ClassPathExclusions("jackson-*.jar")
public class RestClientTestWithoutJacksonIntegrationTests {

	@Test
	public void restClientTestCanBeUsedWhenJacksonIsNotOnTheClassPath() {
		assertThat(ClassUtils.isPresent("com.fasterxml.jackson.databind.Module", getClass().getClassLoader()))
				.isFalse();
		Result result = JUnitCore.runClasses(RestClientTestWithComponentIntegrationTests.class);
		assertThat(result.getFailureCount()).isEqualTo(0);
		assertThat(result.getRunCount()).isGreaterThan(0);
	}

}
