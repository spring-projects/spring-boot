/*
 * Copyright 2012-2019 the original author or authors.
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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.example.RealExampleService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Test to ensure that correct bean is injected when @SpyBean and @Qualifier with bean
 * name is used. In scenarios when the @Qualifier does not specify the bean then the bean
 * with @Primary annotation is injected. The test also verifies that when @SpyBean is used
 * with a bean name then appropriate bean is injected and otherwise the bean with @Primary
 * annotation is used.
 *
 * @author Rafiullah Hamedy
 */
@RunWith(SpringRunner.class)
public class SpyBeanWithQualifierIntegrationTests {

	@SpyBean
	@Qualifier("serviceTwo")
	private RealExampleService serviceTwoViaQualifier;

	@SpyBean
	@Qualifier
	private RealExampleService serviceOneViaPrimary;

	@SpyBean(name = "serviceTwo")
	private RealExampleService serviceTwoViaSpyBean;

	@SpyBean
	private RealExampleService serviceTwoViaSpyBeanPrimary;

	@Test
	public void testSpyBeanAndQualifierWithValueRegisterServiceTwoBean() {
		Assert.assertEquals("ServiceTwo, Greetings!",
				this.serviceTwoViaQualifier.greeting());
	}

	@Test
	public void testSpyBeanAndQualifierWithoutValueRegistersServiceOneBean() {
		Assert.assertEquals("ServiceOne, Greetings!",
				this.serviceOneViaPrimary.greeting());
	}

	@Test
	public void testSpyBeanWithNameRegistersServiceTwoBean() {
		Assert.assertEquals("ServiceTwo, Greetings!",
				this.serviceTwoViaSpyBean.greeting());
	}

	@Test
	public void testSpyBeanWithoutNameRegistersPrimaryServiceOneBean() {
		Assert.assertEquals("ServiceOne, Greetings!",
				this.serviceTwoViaSpyBeanPrimary.greeting());
	}

	@Configuration
	static class Config {

		@Primary
		@Bean
		public RealExampleService serviceOne() {
			return new RealExampleService("ServiceOne, Greetings!");
		}

		@Bean
		public RealExampleService serviceTwo() {
			return new RealExampleService("ServiceTwo, Greetings!");
		}

	}

}
