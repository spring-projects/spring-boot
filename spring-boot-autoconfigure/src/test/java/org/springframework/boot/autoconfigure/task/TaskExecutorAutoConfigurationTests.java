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

package org.springframework.boot.autoconfigure.task;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TaskExecutorAutoConfiguration}.
 *
 * @author Vedran Pavic
 */
public class TaskExecutorAutoConfigurationTests {

	private final AnnotationConfigApplicationContext context =
			new AnnotationConfigApplicationContext();

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void defaultTaskExecutorExists() {
		registerAndRefresh(TaskExecutorAutoConfiguration.class);

		assertThat(this.context.getBean(TaskExecutor.class))
				.isInstanceOf(ThreadPoolTaskExecutor.class);
	}

	@Test
	public void customMaxPoolSize() {
		EnvironmentTestUtils.addEnvironment(this.context, "spring.task.pool.max-size=5");
		registerAndRefresh(TaskExecutorAutoConfiguration.class);

		ThreadPoolTaskExecutor taskExecutor =
				this.context.getBean(ThreadPoolTaskExecutor.class);
		assertThat(taskExecutor.getMaxPoolSize()).isEqualTo(5);
	}

	private void registerAndRefresh(Class<?>... annotatedClasses) {
		this.context.register(annotatedClasses);
		this.context.refresh();
	}

}
