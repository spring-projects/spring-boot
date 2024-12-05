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

package org.springframework.boot.actuate.scheduling;


import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.scheduling.ThreadPoolTaskExecutorsEndpoint.ThreadPoolTaskExecutorDescriptor;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Tests for {@link ThreadPoolTaskExecutorsEndpoint}.
 *
 * @author Tanfei Long
 */
class ThreadPoolTaskExecutorsEndpointTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(BaseConfiguration.class).withBean(ThreadPoolTaskExecutorsEndpoint.class);

	private final String executorName = "customerThreadPool";

	@Test
	void threadPoolTaskExecutorDescriptor() {
		this.contextRunner.run((context) -> {
			ThreadPoolTaskExecutorsEndpoint endpoint = context.getBean(ThreadPoolTaskExecutorsEndpoint.class);
			Map<String, ThreadPoolTaskExecutorDescriptor> descriptorMap = endpoint.threadPoolTaskExecutors();
			ThreadPoolTaskExecutorDescriptor descriptor = descriptorMap.get(this.executorName);
			assertThat(descriptor).isNotNull();
			assertThat(descriptor.getCorePoolSize()).isEqualTo(2);
			assertThat(descriptor.getMaxPoolSize()).isEqualTo(5);
			assertThat(descriptor.getQueueCapacity()).isEqualTo(100);
		});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAsync
	static class BaseConfiguration {

		@Bean("customerThreadPool")
		ThreadPoolTaskExecutor endpoint() {
			ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
			executor.setCorePoolSize(2);
			executor.setMaxPoolSize(5);
			executor.setQueueCapacity(100);
			executor.setThreadNamePrefix("custom-thread-pool-");
			executor.initialize();
			return executor;
		}

	}
}
