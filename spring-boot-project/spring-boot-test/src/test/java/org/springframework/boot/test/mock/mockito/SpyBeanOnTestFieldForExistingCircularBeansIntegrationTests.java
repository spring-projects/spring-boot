/*
 * Copyright 2012-2022 the original author or authors.
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBeanOnTestFieldForExistingCircularBeansIntegrationTests.SpyBeanOnTestFieldForExistingCircularBeansConfig;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.mockito.BDDMockito.then;

/**
 * Test {@link SpyBean @SpyBean} on a test class field can be used to replace existing
 * beans with circular dependencies.
 *
 * @author Andy Wilkinson
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SpyBeanOnTestFieldForExistingCircularBeansConfig.class)
class SpyBeanOnTestFieldForExistingCircularBeansIntegrationTests {

	@SpyBean
	private One one;

	@Autowired
	private Two two;

	@Test
	void beanWithCircularDependenciesCanBeSpied() {
		this.two.callOne();
		then(this.one).should().someMethod();
	}

	@Import({ One.class, Two.class })
	static class SpyBeanOnTestFieldForExistingCircularBeansConfig {

	}

	static class One {

		@Autowired
		@SuppressWarnings("unused")
		private Two two;

		void someMethod() {

		}

	}

	static class Two {

		@Autowired
		private One one;

		void callOne() {
			this.one.someMethod();
		}

	}

}
