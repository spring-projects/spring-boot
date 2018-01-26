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

package org.springframework.boot.actuate.autoconfigure.metrics.export.newrelic;

import io.micrometer.core.instrument.Clock;
import io.micrometer.newrelic.NewRelicConfig;
import io.micrometer.newrelic.NewRelicMeterRegistry;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for exporting metrics to New Relic.
 *
 * @author Jon Schneider
 * @since 2.0.0
 */
@Configuration
@ConditionalOnClass(NewRelicMeterRegistry.class)
@EnableConfigurationProperties(NewRelicProperties.class)
public class NewRelicExportConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public NewRelicConfig newRelicConfig(NewRelicProperties props) {
		return new NewRelicPropertiesConfigAdapter(props);
	}

	@Bean
	@ConditionalOnProperty(value = "management.metrics.export.newrelic.enabled", matchIfMissing = true)
	@ConditionalOnMissingBean
	public NewRelicMeterRegistry newRelicMeterRegistry(NewRelicConfig config, Clock clock) {
		return new NewRelicMeterRegistry(config, clock);
	}

	@Bean
	@ConditionalOnMissingBean
	public Clock micrometerClock() {
		return Clock.SYSTEM;
	}

}
