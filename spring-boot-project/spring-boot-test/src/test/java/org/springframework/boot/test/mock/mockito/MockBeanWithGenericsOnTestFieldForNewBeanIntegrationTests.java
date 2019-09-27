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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.example.ExampleGenericService;
import org.springframework.boot.test.mock.mockito.example.ExampleGenericServiceCaller;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Test {@link MockBean @MockBean} on a test class field can be used to inject new mock
 * instances.
 *
 * @author Phillip Webb
 */
@ExtendWith(SpringExtension.class)
class MockBeanWithGenericsOnTestFieldForNewBeanIntegrationTests {

	@MockBean
	private ExampleGenericService<Integer> exampleIntegerService;

	@MockBean
	private ExampleGenericService<String> exampleStringService;

	@Autowired
	private ExampleGenericServiceCaller caller;

	@Test
	void testMocking() {
		given(this.exampleIntegerService.greeting()).willReturn(200);
		given(this.exampleStringService.greeting()).willReturn("Boot");
		assertThat(this.caller.sayGreeting()).isEqualTo("I say 200 Boot");
	}

	@Configuration(proxyBeanMethods = false)
	@Import(ExampleGenericServiceCaller.class)
	static class Config {

	}

}
