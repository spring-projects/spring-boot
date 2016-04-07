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

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.DefaultManagedTaskExecutor;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for a JNDI located
 * {@link TaskExecutor}.
 *
 * @author Vedran Pavic
 * @since 1.4.0
 */
@Configuration
@AutoConfigureBefore(TaskExecutorAutoConfiguration.class)
@ConditionalOnProperty(prefix = "spring.task", name = "jndi-name")
@EnableConfigurationProperties(TaskExecutorProperties.class)
public class JndiTaskExecutorAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public TaskExecutor taskExecutor(TaskExecutorProperties properties) {
		DefaultManagedTaskExecutor taskExecutor = new DefaultManagedTaskExecutor();
		taskExecutor.setJndiName(properties.getJndiName());
		return taskExecutor;
	}

}
