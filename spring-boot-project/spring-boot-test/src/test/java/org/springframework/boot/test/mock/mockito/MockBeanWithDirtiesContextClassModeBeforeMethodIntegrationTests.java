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
import org.springframework.boot.test.mock.mockito.example.ExampleService;
import org.springframework.boot.test.mock.mockito.example.ExampleServiceCaller;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Integration tests for using {@link MockBean} with {@link DirtiesContext} and
 * {@link ClassMode#BEFORE_EACH_TEST_METHOD}.
 *
 * @author Andy Wilkinson
 */
@RunWith(SpringRunner.class)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
public class MockBeanWithDirtiesContextClassModeBeforeMethodIntegrationTests {

	@MockBean
	private ExampleService exampleService;

	@Autowired
	private ExampleServiceCaller caller;

	@Test
	public void testMocking() throws Exception {
		given(this.exampleService.greeting()).willReturn("Boot");
		assertThat(this.caller.sayGreeting()).isEqualTo("I say Boot");
	}

	@Configuration
	@Import(ExampleServiceCaller.class)
	static class Config {

	}

}
