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

package org.springframework.boot.actuate.autoconfigure.metrics.export.humio;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.ipc.http.HttpUrlConnectionSender;
import io.micrometer.humio.HumioConfig;
import io.micrometer.humio.HumioMeterRegistry;

import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for exporting metrics to Humio.
 *
 * @author Andy Wilkinson
 * @author Artsiom Yudovin
 * @since 2.1.0
 */
@Configuration
@AutoConfigureBefore({ CompositeMeterRegistryAutoConfiguration.class,
		SimpleMetricsExportAutoConfiguration.class })
@AutoConfigureAfter(MetricsAutoConfiguration.class)
@ConditionalOnBean(Clock.class)
@ConditionalOnClass(HumioMeterRegistry.class)
@ConditionalOnProperty(prefix = "management.metrics.export.humio", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(HumioProperties.class)
public class HumioMetricsExportAutoConfiguration {

	private final HumioProperties properties;

	public HumioMetricsExportAutoConfiguration(HumioProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	public HumioConfig humioConfig() {
		return new HumioPropertiesConfigAdapter(this.properties);
	}

	@Bean
	@ConditionalOnMissingBean
	public HumioMeterRegistry humioMeterRegistry(HumioConfig humioConfig, Clock clock) {
		return HumioMeterRegistry.builder(humioConfig).clock(clock)
				.httpClient(
						new HttpUrlConnectionSender(this.properties.getConnectTimeout(),
								this.properties.getReadTimeout()))
				.build();

	}

}
