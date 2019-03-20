/*
 * Copyright 2012-2017 the original author or authors.
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

import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runner.notification.Failure;

import org.springframework.boot.junit.runner.classpath.ClassPathOverrides;
import org.springframework.boot.junit.runner.classpath.ModifiedClassPathRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for compatibility with Mockito 2.1
 *
 * @author Andy Wilkinson
 */
@RunWith(ModifiedClassPathRunner.class)
@ClassPathOverrides("org.mockito:mockito-core:2.1.0")
public class Mockito21Tests {

	@Test
	public void resetMocksTestExecutionListenerTestsWithMockito2() {
		runTests(ResetMocksTestExecutionListenerTests.class);
	}

	@Test
	public void spyBeanWithAopProxyTestsWithMockito2() {
		runTests(SpyBeanWithAopProxyTests.class);
	}

	private void runTests(Class<?> testClass) {
		Result result = new JUnitCore().run(testClass);
		for (Failure failure : result.getFailures()) {
			System.err.println(failure.getTrace());
		}
		assertThat(result.getFailureCount()).isEqualTo(0);
		assertThat(result.getRunCount()).isGreaterThan(0);
	}

}
