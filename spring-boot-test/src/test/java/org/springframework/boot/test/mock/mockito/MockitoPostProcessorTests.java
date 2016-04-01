/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.test.mock.mockito;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.test.mock.mockito.example.ExampleService;
import org.springframework.boot.test.mock.mockito.example.FailingExampleService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Test for {@link MockitoPostProcessor}. See also the integration tests.
 *
 * @author Phillip Webb
 */
public class MockitoPostProcessorTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void cannotMockMultipleBeans() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		MockitoPostProcessor.register(context);
		context.register(MultipleBeans.class);
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage(
				"Unable to register mock bean " + ExampleService.class.getName()
						+ " expected a single existing bean to replace "
						+ "but found [example1, example2]");
		context.refresh();
	}

	@Configuration
	@MockBean(ExampleService.class)
	static class MultipleBeans {

		@Bean
		public ExampleService example1() {
			return new FailingExampleService();
		}

		@Bean
		public ExampleService example2() {
			return new FailingExampleService();
		}

	}

}
