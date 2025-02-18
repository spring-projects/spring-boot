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

package org.springframework.boot.actuate.autoconfigure.observation.batch;

import io.micrometer.observation.ObservationRegistry;

import org.springframework.batch.core.configuration.annotation.BatchObservabilityBeanPostProcessor;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for instrumentation of Spring Batch
 * Jobs.
 *
 * @author Mark Bonnekessel
 * @since 3.0.6
 */
@AutoConfiguration(after = ObservationAutoConfiguration.class)
@ConditionalOnBean(ObservationRegistry.class)
@ConditionalOnClass({ ObservationRegistry.class, BatchObservabilityBeanPostProcessor.class })
public class BatchObservationAutoConfiguration {

	@ConditionalOnMissingBean
	@Bean
	public static BatchObservabilityBeanPostProcessor batchObservabilityBeanPostProcessor() {
		return new BatchObservabilityBeanPostProcessor();
	}

}
