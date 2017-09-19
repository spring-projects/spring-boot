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

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBeanOnContextHierarchyIntegrationTests.ChildConfig;
import org.springframework.boot.test.mock.mockito.MockBeanOnContextHierarchyIntegrationTests.ParentConfig;
import org.springframework.boot.test.mock.mockito.example.ExampleService;
import org.springframework.boot.test.mock.mockito.example.ExampleServiceCaller;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test {@link MockBean} can be used with a {@link ContextHierarchy}.
 *
 * @author Phillip Webb
 */
@RunWith(SpringRunner.class)
@ContextHierarchy({ @ContextConfiguration(classes = ParentConfig.class),
		@ContextConfiguration(classes = ChildConfig.class) })
public class MockBeanOnContextHierarchyIntegrationTests {

	@Autowired
	private ChildConfig childConfig;

	@Test
	public void testMocking() throws Exception {
		ApplicationContext context = this.childConfig.getContext();
		ApplicationContext parentContext = context.getParent();
		assertThat(parentContext.getBeanNamesForType(ExampleService.class)).hasSize(1);
		assertThat(parentContext.getBeanNamesForType(ExampleServiceCaller.class))
				.hasSize(0);
		assertThat(context.getBeanNamesForType(ExampleService.class)).hasSize(0);
		assertThat(context.getBeanNamesForType(ExampleServiceCaller.class)).hasSize(1);
		assertThat(context.getBean(ExampleService.class)).isNotNull();
		assertThat(context.getBean(ExampleServiceCaller.class)).isNotNull();
	}

	@Configuration
	@MockBean(ExampleService.class)
	static class ParentConfig {

	}

	@Configuration
	@MockBean(ExampleServiceCaller.class)
	static class ChildConfig implements ApplicationContextAware {

		private ApplicationContext context;

		@Override
		public void setApplicationContext(ApplicationContext applicationContext)
				throws BeansException {
			this.context = applicationContext;
		}

		public ApplicationContext getContext() {
			return this.context;
		}

	}

}
