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

package org.springframework.boot.actuate.autoconfigure.influx;

import java.util.Map;

import org.influxdb.InfluxDB;

import org.springframework.boot.actuate.autoconfigure.health.CompositeHealthIndicatorConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.autoconfigure.health.HealthIndicatorAutoConfiguration;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.influx.InfluxDbHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.influx.InfluxDbAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link InfluxDbHealthIndicator}.
 *
 * @author Eddú Meléndez
 * @since 2.0.0
 */
@Configuration
@ConditionalOnClass(InfluxDB.class)
@ConditionalOnBean(InfluxDB.class)
@ConditionalOnEnabledHealthIndicator("influxdb")
@AutoConfigureBefore(HealthIndicatorAutoConfiguration.class)
@AutoConfigureAfter(InfluxDbAutoConfiguration.class)
public class InfluxDbHealthIndicatorAutoConfiguration
		extends CompositeHealthIndicatorConfiguration<InfluxDbHealthIndicator, InfluxDB> {

	private final Map<String, InfluxDB> influxDbs;

	public InfluxDbHealthIndicatorAutoConfiguration(Map<String, InfluxDB> influxDbs) {
		this.influxDbs = influxDbs;
	}

	@Bean
	@ConditionalOnMissingBean(name = "influxDbHealthIndicator")
	public HealthIndicator influxDbHealthIndicator() {
		return createHealthIndicator(this.influxDbs);
	}

}
