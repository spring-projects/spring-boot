/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.test.web.client;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.test.context.MergedContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TestRestTemplateContextCustomizer}.
 *
 * @author Andy Wilkinson
 */
class TestRestTemplateContextCustomizerTests {

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void whenContextIsNotABeanDefinitionRegistryTestRestTemplateIsRegistered() {
		new ApplicationContextRunner(TestApplicationContext::new).withInitializer((context) -> {
			MergedContextConfiguration configuration = mock(MergedContextConfiguration.class);
			given(configuration.getTestClass()).willReturn((Class) TestClass.class);
			new TestRestTemplateContextCustomizer().customizeContext(context, configuration);
		}).run((context) -> assertThat(context).hasSingleBean(TestRestTemplate.class));
	}

	@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
	static class TestClass {

	}

	static class TestApplicationContext extends AbstractApplicationContext {

		private final ConfigurableListableBeanFactory beanFactory = new DefaultListableBeanFactory();

		@Override
		protected void refreshBeanFactory() {
		}

		@Override
		protected void closeBeanFactory() {

		}

		@Override
		public ConfigurableListableBeanFactory getBeanFactory() {
			return this.beanFactory;
		}

	}

}
