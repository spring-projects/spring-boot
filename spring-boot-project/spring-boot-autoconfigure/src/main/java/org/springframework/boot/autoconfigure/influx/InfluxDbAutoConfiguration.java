/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.influx;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.InfluxDBClientOptions;
import com.influxdb.client.InfluxDBClientOptions.Builder;
import okhttp3.OkHttpClient;
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
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for InfluxDB.
 *
 * @author Sergey Kuptsov
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 * @since 2.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty("spring.influx.url")
@EnableConfigurationProperties(InfluxDbProperties.class)
public class InfluxDbAutoConfiguration {

	private static OkHttpClient.Builder determineBuilder(InfluxDbOkHttpClientBuilderProvider builder) {
		if (builder != null) {
			return builder.get();
		}
		return new OkHttpClient.Builder();
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(InfluxDB.class)
	static class Influx1xConfiguration {

		@Bean
		@ConditionalOnMissingBean
		InfluxDB influxDb(InfluxDbProperties properties, ObjectProvider<InfluxDbOkHttpClientBuilderProvider> builder,
				ObjectProvider<InfluxDbCustomizer> customizers) {
			InfluxDB influxDb = new InfluxDBImpl(properties.getUrl(), properties.getUser(), properties.getPassword(),
					determineBuilder(builder.getIfAvailable()));
			customizers.orderedStream().forEach((customizer) -> customizer.customize(influxDb));
			return influxDb;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(InfluxDBClient.class)
	static class Influx2xConfiguration {

		@Bean
		@ConditionalOnMissingBean
		InfluxDBClient influxDbClient(InfluxDbProperties properties,
				ObjectProvider<InfluxDbOkHttpClientBuilderProvider> httpClientBuilder,
				ObjectProvider<InfluxDbClientOptionsBuilderCustomizer> customizers) {
			Builder builder = InfluxDBClientOptions.builder().url(properties.getUrl());
			if (StringUtils.hasText(properties.getUser()) && StringUtils.hasText(properties.getPassword())) {
				builder.authenticate(properties.getUser(), properties.getPassword().toCharArray());
			}
			builder.okHttpClient(determineBuilder(httpClientBuilder.getIfAvailable()));
			customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
			return InfluxDBClientFactory.create(builder.build());
		}

	}

}
