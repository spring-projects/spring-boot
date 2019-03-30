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

package org.springframework.boot.test.mock.mockito;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.example.ExampleGenericService;
import org.springframework.boot.test.mock.mockito.example.ExampleGenericServiceCaller;
import org.springframework.boot.test.mock.mockito.example.SimpleExampleIntegerGenericService;
import org.springframework.boot.test.mock.mockito.example.SimpleExampleStringGenericService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Test {@link SpyBean} on a test class field can be used to replace existing beans.
 *
 * @author Phillip Webb
 * @see SpyBeanOnTestFieldForExistingBeanCacheIntegrationTests
 */
@RunWith(SpringRunner.class)
public class SpyBeanOnTestFieldForExistingGenericBeanIntegrationTests {

	// gh-7625

	@SpyBean
	private ExampleGenericService<String> exampleService;

	@Autowired
	private ExampleGenericServiceCaller caller;

	@Test
	public void testSpying() {
		assertThat(this.caller.sayGreeting()).isEqualTo("I say 123 simple");
		verify(this.exampleService).greeting();
	}

	@Configuration(proxyBeanMethods = false)
	@Import({ ExampleGenericServiceCaller.class,
			SimpleExampleIntegerGenericService.class })
	static class SpyBeanOnTestFieldForExistingBeanConfig {

		@Bean
		public ExampleGenericService<String> simpleExampleStringGenericService() {
			// In order to trigger issue we need a method signature that returns the
			// generic type not the actual implementation class
			return new SimpleExampleStringGenericService();
		}

	}

}
