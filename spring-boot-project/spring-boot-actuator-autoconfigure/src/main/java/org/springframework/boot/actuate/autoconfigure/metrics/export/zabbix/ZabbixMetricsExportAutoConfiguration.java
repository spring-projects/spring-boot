/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.export.zabbix;

import io.micrometer.zabbix.NoopZabbixDiscoveryProvider;
import io.micrometer.zabbix.ZabbixConfig;
import io.micrometer.zabbix.ZabbixDiscoveryProvider;
import io.micrometer.zabbix.ZabbixMeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.ConditionalOnEnabledMetricsExport;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.Clock;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for exporting metrics to Zabbix.
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore({ CompositeMeterRegistryAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class })
@AutoConfigureAfter(MetricsAutoConfiguration.class)
@ConditionalOnBean(Clock.class)
@ConditionalOnClass(ZabbixMeterRegistry.class)
@ConditionalOnEnabledMetricsExport("zabbix")
@EnableConfigurationProperties(ZabbixProperties.class)
public class ZabbixMetricsExportAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public ZabbixDiscoveryProvider zabbixDiscoveryProvider() {
		return new NoopZabbixDiscoveryProvider();
	}

	@Bean
	@ConditionalOnMissingBean
	public ZabbixConfig zabbixConfig(ZabbixProperties zabbixProperties) {
		return new ZabbixPropertiesConfigAdapter(zabbixProperties);
	}

	@Bean
	@ConditionalOnMissingBean
	public ZabbixMeterRegistry zabbixMeterRegistry(ZabbixConfig zabbixConfig, Clock clock,
			ZabbixDiscoveryProvider zabbixDiscoveryProvider) {
		return new ZabbixMeterRegistry(zabbixConfig, clock, zabbixDiscoveryProvider);
	}

}