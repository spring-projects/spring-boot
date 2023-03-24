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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MockBean} with abstract class and generics.
 *
 * @param <T> type of thing
 * @param <U> type of something
 * @author Madhura Bhave
 */
@SpringBootTest(classes = AbstractMockBeanOnGenericTests.TestConfiguration.class)
abstract class AbstractMockBeanOnGenericTests<T extends AbstractMockBeanOnGenericTests.Thing<U>, U extends AbstractMockBeanOnGenericTests.Something> {

	@Autowired
	@SuppressWarnings("unused")
	private T thing;

	@MockBean
	private U something;

	@Test
	void mockBeanShouldResolveConcreteType() {
		assertThat(this.something).isInstanceOf(SomethingImpl.class);
	}

	abstract static class Thing<T extends AbstractMockBeanOnGenericTests.Something> {

		@Autowired
		private T something;

		T getSomething() {
			return this.something;
		}

		void setSomething(T something) {
			this.something = something;
		}

	}

	static class SomethingImpl extends Something {

	}

	static class ThingImpl extends Thing<SomethingImpl> {

	}

	static class Something {

	}

	@Configuration
	static class TestConfiguration {

		@Bean
		ThingImpl thing() {
			return new ThingImpl();
		}

	}

}
