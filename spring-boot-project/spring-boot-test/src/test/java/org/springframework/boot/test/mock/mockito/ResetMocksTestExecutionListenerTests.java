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

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.example.ExampleService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ResetMocksTestExecutionListener}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
@RunWith(SpringRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ResetMocksTestExecutionListenerTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void test001() {
		given(getMock("none").greeting()).willReturn("none");
		given(getMock("before").greeting()).willReturn("before");
		given(getMock("after").greeting()).willReturn("after");
	}

	@Test
	public void test002() {
		assertThat(getMock("none").greeting()).isEqualTo("none");
		assertThat(getMock("before").greeting()).isNull();
		assertThat(getMock("after").greeting()).isNull();
	}

	public ExampleService getMock(String name) {
		return this.context.getBean(name, ExampleService.class);
	}

	@Configuration
	static class Config {

		@Bean
		public ExampleService before(MockitoBeans mockedBeans) {
			ExampleService mock = mock(ExampleService.class, MockReset.before());
			mockedBeans.add(mock);
			return mock;
		}

		@Bean
		public ExampleService after(MockitoBeans mockedBeans) {
			ExampleService mock = mock(ExampleService.class, MockReset.after());
			mockedBeans.add(mock);
			return mock;
		}

		@Bean
		public ExampleService none(MockitoBeans mockedBeans) {
			ExampleService mock = mock(ExampleService.class);
			mockedBeans.add(mock);
			return mock;
		}

		@Bean
		@Lazy
		public ExampleService fail() {
			// gh-5870
			throw new RuntimeException();
		}

		@Bean
		public BrokenFactoryBean brokenFactoryBean() {
			// gh-7270
			return new BrokenFactoryBean();
		}

	}

	static class BrokenFactoryBean implements FactoryBean<String> {

		@Override
		public String getObject() throws Exception {
			throw new IllegalStateException();
		}

		@Override
		public Class<?> getObjectType() {
			return String.class;
		}

		@Override
		public boolean isSingleton() {
			return true;
		}

	}

}
