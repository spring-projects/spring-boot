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
import org.mockito.internal.util.MockUtil;

import org.springframework.boot.test.mock.mockito.example.SimpleExampleStringGenericService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test {@link SpyBean} on a test class field can be used to inject a spy instance when
 * there are multiple candidates and one is chosen using the name attribute.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
@RunWith(SpringRunner.class)
public class SpyBeanWithNameOnTestFieldForMultipleExistingBeansTests {

	@SpyBean(name = "two")
	private SimpleExampleStringGenericService spy;

	@Test
	public void testSpying() throws Exception {
		assertThat(MockUtil.isSpy(this.spy)).isTrue();
		assertThat(MockUtil.getMockName(this.spy).toString()).isEqualTo("two");
	}

	@Configuration
	static class Config {

		@Bean
		public SimpleExampleStringGenericService one() {
			return new SimpleExampleStringGenericService("one");
		}

		@Bean
		public SimpleExampleStringGenericService two() {
			return new SimpleExampleStringGenericService("two");
		}

	}

}
