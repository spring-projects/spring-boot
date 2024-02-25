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

package org.springframework.boot.actuate.autoconfigure.metrics.export.elastic;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.ipc.http.HttpUrlConnectionSender;
import io.micrometer.elastic.ElasticConfig;
import io.micrometer.elastic.ElasticMeterRegistry;

import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.ConditionalOnEnabledMetricsExport;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.source.MutuallyExclusiveConfigurationPropertiesException;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for exporting metrics to Elastic.
 *
 * @author Andy Wilkinson
 * @author Artsiom Yudovin
 * @since 2.1.0
 */
@AutoConfiguration(
		before = { CompositeMeterRegistryAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class },
		after = MetricsAutoConfiguration.class)
@ConditionalOnBean(Clock.class)
@ConditionalOnClass(ElasticMeterRegistry.class)
@ConditionalOnEnabledMetricsExport("elastic")
@EnableConfigurationProperties(ElasticProperties.class)
public class ElasticMetricsExportAutoConfiguration {

	private final ElasticProperties properties;

	/**
     * Constructs a new instance of ElasticMetricsExportAutoConfiguration with the specified ElasticProperties.
     *
     * @param properties the ElasticProperties to be used for configuration
     */
    public ElasticMetricsExportAutoConfiguration(ElasticProperties properties) {
		this.properties = properties;
	}

	/**
     * Creates an instance of ElasticConfig if no other bean of the same type is present.
     * 
     * This method checks for mutually exclusive configuration properties and throws an exception if multiple non-null values are found.
     * 
     * The first check is for the "api-key-credentials" and "user-name" properties. If both properties have non-null values, an exception is thrown.
     * 
     * The second check is for the "api-key-credentials" and "password" properties. If both properties have non-null values, an exception is thrown.
     * 
     * Finally, an instance of ElasticPropertiesConfigAdapter is created using the properties provided.
     * 
     * @return an instance of ElasticConfig
     */
    @Bean
	@ConditionalOnMissingBean
	public ElasticConfig elasticConfig() {
		MutuallyExclusiveConfigurationPropertiesException.throwIfMultipleNonNullValuesIn((entries) -> {
			entries.put("api-key-credentials", this.properties.getApiKeyCredentials());
			entries.put("user-name", this.properties.getUserName());
		});
		MutuallyExclusiveConfigurationPropertiesException.throwIfMultipleNonNullValuesIn((entries) -> {
			entries.put("api-key-credentials", this.properties.getApiKeyCredentials());
			entries.put("password", this.properties.getPassword());
		});
		return new ElasticPropertiesConfigAdapter(this.properties);
	}

	/**
     * Creates an instance of ElasticMeterRegistry if no other bean of the same type is present.
     * 
     * @param elasticConfig The ElasticConfig object containing the configuration for the ElasticMeterRegistry.
     * @param clock The Clock object used for timekeeping.
     * @return An instance of ElasticMeterRegistry.
     */
    @Bean
	@ConditionalOnMissingBean
	public ElasticMeterRegistry elasticMeterRegistry(ElasticConfig elasticConfig, Clock clock) {
		return ElasticMeterRegistry.builder(elasticConfig)
			.clock(clock)
			.httpClient(
					new HttpUrlConnectionSender(this.properties.getConnectTimeout(), this.properties.getReadTimeout()))
			.build();
	}

}
