/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.logging.Log4j2Metrics;
import org.apache.logging.log4j.core.LoggerContext;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for Log4J2 metrics.
 *
 * @author Andy Wilkinson
 * @since 2.1.0
 */
@Configuration
@AutoConfigureAfter(MetricsAutoConfiguration.class)
@ConditionalOnClass({ Log4j2Metrics.class, LoggerContext.class })
@ConditionalOnBean(MeterRegistry.class)
public class Log4J2MetricsAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public Log4j2Metrics log4j2Metrics() {
		return new Log4j2Metrics();
	}

}
