/*
 * Copyright 2012-2013 the original author or authors.
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
 *
 */

package org.springframework.boot.autoconfigure.task;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Test;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

/**
 * Test case for {@link AsyncTaskExecutorAutoConfiguration}
 *
 * @author Tiarê Balbi Bonamini
 * @since 1.3.0
 */
public class AsyncTaskExecutorAutoConfigurationTest {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {

		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testTaskExecutorCreated() {
		this.context = new AnnotationConfigApplicationContext();

		this.context.register(PropertyPlaceholderAutoConfiguration.class,
				AsyncTaskExecutorAutoConfiguration.class);
		this.context.refresh();

		AsyncTaskExecutor bean = this.context.getBean(AsyncTaskExecutor.class);

		assertNotNull(bean);
		assertTrue(bean instanceof SimpleAsyncTaskExecutor);

	}

	@Test
	public void testCustomProperties() {

		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.task.concurrency-limit:30",
				"spring.task.thread-name-prefix:spring");

		this.context.register(PropertyPlaceholderAutoConfiguration.class,
				AsyncTaskExecutorAutoConfiguration.class);

		this.context.refresh();

		SimpleAsyncTaskExecutor bean = (SimpleAsyncTaskExecutor) this.context
				.getBean(AsyncTaskExecutor.class);
		assertEquals(30, bean.getConcurrencyLimit());
		assertEquals("spring", bean.getThreadNamePrefix());

	}

}