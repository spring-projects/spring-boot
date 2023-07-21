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

package org.springframework.boot.actuate.autoconfigure.influx;

import java.util.Map;

import org.influxdb.InfluxDB;

import org.springframework.boot.actuate.autoconfigure.health.CompositeHealthContributorConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.influx.InfluxDbHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.influx.InfluxDbAutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link InfluxDbHealthIndicator}.
 *
 * @author Eddú Meléndez
 * @since 2.0.0
 * @deprecated since 3.2.0 for removal in 3.4.0 in favor of the
 * <a href="https://github.com/influxdata/influxdb-client-java">new client</a> and its own
 * Spring Boot integration.
 */
@SuppressWarnings("removal")
@AutoConfiguration(after = InfluxDbAutoConfiguration.class)
@ConditionalOnClass(InfluxDB.class)
@ConditionalOnBean(InfluxDB.class)
@ConditionalOnEnabledHealthIndicator("influxdb")
@Deprecated(since = "3.2.0", forRemoval = true)
public class InfluxDbHealthContributorAutoConfiguration
		extends CompositeHealthContributorConfiguration<InfluxDbHealthIndicator, InfluxDB> {

	public InfluxDbHealthContributorAutoConfiguration() {
		super(InfluxDbHealthIndicator::new);
	}

	@Bean
	@ConditionalOnMissingBean(name = { "influxDbHealthIndicator", "influxDbHealthContributor" })
	public HealthContributor influxDbHealthContributor(Map<String, InfluxDB> influxDbs) {
		return createContributor(influxDbs);
	}

}
