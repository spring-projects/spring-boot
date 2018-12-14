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

package org.springframework.boot.autoconfigure.influx;

import okhttp3.OkHttpClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.influxdb.InfluxDB;
import org.influxdb.impl.InfluxDBImpl;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for InfluxDB.
 *
 * @author Sergey Kuptsov
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 * @since 2.0.0
 */
@Configuration
@ConditionalOnClass(InfluxDB.class)
@EnableConfigurationProperties(InfluxDbProperties.class)
public class InfluxDbAutoConfiguration {

	private static final Log logger = LogFactory.getLog(InfluxDbAutoConfiguration.class);

	private final InfluxDbProperties properties;

	private final OkHttpClient.Builder builder;

	public InfluxDbAutoConfiguration(InfluxDbProperties properties,
			ObjectProvider<InfluxDbOkHttpClientBuilderProvider> builder,
			ObjectProvider<OkHttpClient.Builder> deprecatedBuilder) {
		this.properties = properties;
		this.builder = determineBuilder(builder.getIfAvailable(),
				deprecatedBuilder.getIfAvailable());
	}

	@Deprecated
	private static OkHttpClient.Builder determineBuilder(
			InfluxDbOkHttpClientBuilderProvider builder,
			OkHttpClient.Builder deprecatedBuilder) {
		if (builder != null) {
			return builder.get();
		}
		else if (deprecatedBuilder != null) {
			logger.warn(
					"InfluxDB client customizations using a OkHttpClient.Builder is deprecated, register a "
							+ InfluxDbOkHttpClientBuilderProvider.class.getSimpleName()
							+ " bean instead");
			return deprecatedBuilder;
		}
		return new OkHttpClient.Builder();
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty("spring.influx.url")
	public InfluxDB influxDb() {
		return new InfluxDBImpl(this.properties.getUrl(), this.properties.getUser(),
				this.properties.getPassword(), this.builder);
	}

}
