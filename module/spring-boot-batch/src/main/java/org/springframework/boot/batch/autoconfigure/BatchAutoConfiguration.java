/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.batch.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Batch using an in-memory
 * store.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass(JobOperator.class)
@ConditionalOnMissingBean(value = DefaultBatchConfiguration.class, annotation = EnableBatchProcessing.class)
@EnableConfigurationProperties(BatchProperties.class)
public final class BatchAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	static class SpringBootBatchDefaultConfiguration extends DefaultBatchConfiguration {

		private final @Nullable TaskExecutor taskExecutor;

		private final @Nullable JobParametersConverter jobParametersConverter;

		SpringBootBatchDefaultConfiguration(@BatchTaskExecutor ObjectProvider<TaskExecutor> batchTaskExecutor,
				ObjectProvider<JobParametersConverter> jobParametersConverter) {
			this.taskExecutor = batchTaskExecutor.getIfAvailable();
			this.jobParametersConverter = jobParametersConverter.getIfAvailable();
		}

		@Override
		@Deprecated(since = "4.0.0", forRemoval = true)
		@SuppressWarnings("removal")
		protected JobParametersConverter getJobParametersConverter() {
			return (this.jobParametersConverter != null) ? this.jobParametersConverter
					: super.getJobParametersConverter();
		}

		@Override
		protected TaskExecutor getTaskExecutor() {
			return (this.taskExecutor != null) ? this.taskExecutor : super.getTaskExecutor();
		}

	}

}
