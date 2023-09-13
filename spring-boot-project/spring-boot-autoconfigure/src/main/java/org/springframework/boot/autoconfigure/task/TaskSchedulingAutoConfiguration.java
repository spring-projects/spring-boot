/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.task;

import org.springframework.boot.LazyInitializationExcludeFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.TaskManagementConfigUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link TaskScheduler}.
 *
 * @author Stephane Nicoll
 * @author Moritz Halbritter
 * @since 2.1.0
 */
@ConditionalOnClass(ThreadPoolTaskScheduler.class)
@AutoConfiguration(after = TaskExecutionAutoConfiguration.class)
@EnableConfigurationProperties(TaskSchedulingProperties.class)
@Import({ TaskSchedulingConfigurations.ThreadPoolTaskSchedulerBuilderConfiguration.class,
		TaskSchedulingConfigurations.TaskSchedulerBuilderConfiguration.class,
		TaskSchedulingConfigurations.SimpleAsyncTaskSchedulerBuilderConfiguration.class,
		TaskSchedulingConfigurations.TaskSchedulerConfiguration.class })
public class TaskSchedulingAutoConfiguration {

	@Bean
	@ConditionalOnBean(name = TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME)
	public static LazyInitializationExcludeFilter scheduledBeanLazyInitializationExcludeFilter() {
		return new ScheduledBeanLazyInitializationExcludeFilter();
	}

}
