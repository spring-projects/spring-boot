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

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.CustomScopeConfigurer;
import org.springframework.boot.test.mock.mockito.SpyBeanOnTestFieldForExistingScopedBeanIntegrationTests.SpyBeanOnTestFieldForExistingScopedBeanConfig;
import org.springframework.boot.test.mock.mockito.example.ExampleService;
import org.springframework.boot.test.mock.mockito.example.ExampleServiceCaller;
import org.springframework.boot.test.mock.mockito.example.SimpleExampleService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Test {@link SpyBean @SpyBean} on a test class field can be used to replace existing
 * scoped beans.
 *
 * @author Andy Wilkinson
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SpyBeanOnTestFieldForExistingScopedBeanConfig.class)
public class SpyBeanOnTestFieldForExistingScopedBeanIntegrationTests {

	@SpyBean
	private ExampleService exampleService;

	@Autowired
	private ExampleServiceCaller caller;

	@Test
	void testSpying() {
		assertThat(this.caller.sayGreeting()).isEqualTo("I say simple");
		verify(this.exampleService).greeting();
	}

	@Configuration(proxyBeanMethods = false)
	@Import({ ExampleServiceCaller.class })
	static class SpyBeanOnTestFieldForExistingScopedBeanConfig {

		@Bean
		@Scope(scopeName = "custom", proxyMode = ScopedProxyMode.TARGET_CLASS)
		SimpleExampleService simpleExampleService() {
			return new SimpleExampleService();
		}

		@Bean
		static CustomScopeConfigurer customScopeConfigurer() {
			CustomScopeConfigurer configurer = new CustomScopeConfigurer();
			configurer.addScope("custom", new org.springframework.beans.factory.config.Scope() {

				private Object bean;

				@Override
				public Object resolveContextualObject(String key) {
					throw new UnsupportedOperationException();
				}

				@Override
				public Object remove(String name) {
					throw new UnsupportedOperationException();
				}

				@Override
				public void registerDestructionCallback(String name, Runnable callback) {
					throw new UnsupportedOperationException();
				}

				@Override
				public String getConversationId() {
					throw new UnsupportedOperationException();
				}

				@Override
				public Object get(String name, ObjectFactory<?> objectFactory) {
					if (this.bean == null) {
						this.bean = objectFactory.getObject();
					}
					return this.bean;
				}

			});
			return configurer;
		}

	}

}
