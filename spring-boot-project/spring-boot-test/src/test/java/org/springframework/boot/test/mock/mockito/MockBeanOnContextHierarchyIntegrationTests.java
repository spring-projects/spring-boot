/*
 * Copyright 2012-2024 the original author or authors.
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
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test {@link MockBean @MockBean} can be used with a
 * {@link ContextHierarchy @ContextHierarchy}.
 *
 * @author Phillip Webb
 * @deprecated since 3.4.0 for removal in 3.6.0
 */
@SuppressWarnings("removal")
@Deprecated(since = "3.4.0", forRemoval = true)
@ExtendWith(SpringExtension.class)
@ContextHierarchy({ @ContextConfiguration(classes = MockBeanOnContextHierarchyIntegrationTests.ParentConfig.class),
		@ContextConfiguration(classes = MockBeanOnContextHierarchyIntegrationTests.ChildConfig.class) })
class MockBeanOnContextHierarchyIntegrationTests {

	@Autowired
	private ChildConfig childConfig;

	@Test
	void testMocking() {
		ApplicationContext context = this.childConfig.getContext();
		ApplicationContext parentContext = context.getParent();
		assertThat(parentContext
			.getBeanNamesForType(org.springframework.boot.test.mock.mockito.example.ExampleService.class)).hasSize(1);
		assertThat(parentContext
			.getBeanNamesForType(org.springframework.boot.test.mock.mockito.example.ExampleServiceCaller.class))
			.isEmpty();
		assertThat(context.getBeanNamesForType(org.springframework.boot.test.mock.mockito.example.ExampleService.class))
			.isEmpty();
		assertThat(context
			.getBeanNamesForType(org.springframework.boot.test.mock.mockito.example.ExampleServiceCaller.class))
			.hasSize(1);
		assertThat(context.getBean(org.springframework.boot.test.mock.mockito.example.ExampleService.class))
			.isNotNull();
		assertThat(context.getBean(org.springframework.boot.test.mock.mockito.example.ExampleServiceCaller.class))
			.isNotNull();
	}

	@Configuration(proxyBeanMethods = false)
	@MockBean(org.springframework.boot.test.mock.mockito.example.ExampleService.class)
	static class ParentConfig {

	}

	@Configuration(proxyBeanMethods = false)
	@MockBean(org.springframework.boot.test.mock.mockito.example.ExampleServiceCaller.class)
	static class ChildConfig implements ApplicationContextAware {

		private ApplicationContext context;

		@Override
		public void setApplicationContext(ApplicationContext applicationContext) {
			this.context = applicationContext;
		}

		ApplicationContext getContext() {
			return this.context;
		}

	}

}
