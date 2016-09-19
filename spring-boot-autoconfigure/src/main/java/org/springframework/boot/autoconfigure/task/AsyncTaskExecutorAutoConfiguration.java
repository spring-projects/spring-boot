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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Async Task Executor.
 *
 * @author Tiarê Balbi Bonamini
 * @see EnableAsync
 * @see AsyncTaskExecutor
 * @see SimpleAsyncTaskExecutor
 */
@Configuration
@ConditionalOnBean(annotation = EnableAsync.class)
@EnableConfigurationProperties(AsyncTaskExecutorProperties.class)
public class AsyncTaskExecutorAutoConfiguration {

	@Autowired
	private AsyncTaskExecutorProperties taskProperties;

	@Bean
	@ConditionalOnMissingBean
	public AsyncTaskExecutor simpleAsyncTaskExecutor() {
		SimpleAsyncTaskExecutor asyncTaskExecutor = new SimpleAsyncTaskExecutor();
		asyncTaskExecutor.setConcurrencyLimit(taskProperties.getConcurrencyLimit());
		asyncTaskExecutor.setThreadNamePrefix(taskProperties.getThreadNamePrefix());
		return asyncTaskExecutor;
	}
}