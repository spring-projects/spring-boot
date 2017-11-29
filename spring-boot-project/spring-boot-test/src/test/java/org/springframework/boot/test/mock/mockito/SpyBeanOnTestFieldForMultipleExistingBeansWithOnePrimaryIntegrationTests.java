/*
 * Copyright 2012-2017 the original author or authors.
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.example.ExampleGenericStringServiceCaller;
import org.springframework.boot.test.mock.mockito.example.SimpleExampleStringGenericService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Test {@link SpyBean} on a test class field can be used to inject a spy instance when
 * there are multiple candidates and one is primary.
 *
 * @author Phillip Webb
 */
@RunWith(SpringRunner.class)
public class SpyBeanOnTestFieldForMultipleExistingBeansWithOnePrimaryIntegrationTests {

	@SpyBean
	private SimpleExampleStringGenericService spy;

	@Autowired
	private ExampleGenericStringServiceCaller caller;

	@Test
	public void testSpying() throws Exception {
		assertThat(this.caller.sayGreeting()).isEqualTo("I say two");
		assertThat(Mockito.mockingDetails(this.spy).getMockCreationSettings()
				.getMockName().toString()).isEqualTo("two");
		verify(this.spy).greeting();
	}

	@Configuration
	@Import(ExampleGenericStringServiceCaller.class)
	static class Config {

		@Bean
		public SimpleExampleStringGenericService one() {
			return new SimpleExampleStringGenericService("one");
		}

		@Bean
		@Primary
		public SimpleExampleStringGenericService two() {
			return new SimpleExampleStringGenericService("two");
		}

	}

}
