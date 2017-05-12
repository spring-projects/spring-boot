/*
 * Copyright 2012-2017 the original author or authors.
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

import com.google.common.base.Strings;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for InfluxDB.
 *
 * @author Sergey Kuptsov
 */
@Configuration
@ConditionalOnClass(InfluxDB.class)
@EnableConfigurationProperties(InfluxDBProperties.class)
public class InfluxDBAutoConfiguration {

	private final InfluxDBProperties properties;

	public InfluxDBAutoConfiguration(InfluxDBProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	public InfluxDB influxDB() {
		if (Strings.isNullOrEmpty(this.properties.getUser())) {
			return InfluxDBFactory.connect(
					this.properties.getUrl()
			);
		}
		else {
			return InfluxDBFactory.connect(
					this.properties.getUrl(),
					this.properties.getUser(),
					this.properties.getPassword()
			);
		}
	}
}
