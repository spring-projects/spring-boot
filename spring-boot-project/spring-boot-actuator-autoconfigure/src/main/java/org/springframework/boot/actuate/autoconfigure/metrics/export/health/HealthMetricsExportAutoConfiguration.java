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

package org.springframework.boot.actuate.autoconfigure.metrics.export.health;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.BaseUnits;
import io.micrometer.core.instrument.config.NamingConvention;
import io.micrometer.health.HealthConfig;
import io.micrometer.health.HealthMeterRegistry;
import io.micrometer.health.ServiceLevelObjective;
import io.micrometer.health.objectives.JvmServiceLevelObjectives;
import io.micrometer.health.objectives.OperatingSystemServiceLevelObjectives;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.CompositeHealthContributor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.Status;
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
import org.springframework.context.support.GenericApplicationContext;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for building health indicators based
 * on service level objectives.
 *
 * @author Jon Schneider
 * @since 2.4.0
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore({ CompositeMeterRegistryAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class })
@AutoConfigureAfter(MetricsAutoConfiguration.class)
@ConditionalOnBean(Clock.class)
@ConditionalOnClass(HealthMeterRegistry.class)
@ConditionalOnProperty(prefix = "management.metrics.export.health", name = "enabled", havingValue = "true",
		matchIfMissing = true)
@EnableConfigurationProperties(HealthProperties.class)
public class HealthMetricsExportAutoConfiguration {

	private final NamingConvention camelCasedHealthIndicatorNames = NamingConvention.camelCase;

	private final HealthProperties properties;

	public HealthMetricsExportAutoConfiguration(HealthProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	public HealthConfig healthConfig() {
		return new HealthPropertiesConfigAdapter(this.properties);
	}

	@Bean
	@ConditionalOnMissingBean
	public HealthMeterRegistry healthMeterRegistry(HealthConfig healthConfig, Clock clock,
			ObjectProvider<ServiceLevelObjective> serviceLevelObjectives,
			GenericApplicationContext applicationContext) {
		HealthMeterRegistry registry = HealthMeterRegistry.builder(healthConfig).clock(clock)
				.serviceLevelObjectives(serviceLevelObjectives.orderedStream().toArray(ServiceLevelObjective[]::new))
				.serviceLevelObjectives(JvmServiceLevelObjectives.MEMORY)
				.serviceLevelObjectives(OperatingSystemServiceLevelObjectives.DISK).serviceLevelObjectives(
						this.properties.getApiErrorBudgets().entrySet().stream().map((apiErrorBudget) -> {
							String apiEndpoints = '/' + apiErrorBudget.getKey().replace('.', '/');

							return ServiceLevelObjective.build("api.error.ratio." + apiErrorBudget.getKey())
									.failedMessage("API error ratio exceeded.").baseUnit(BaseUnits.PERCENT)
									.tag("uri.matches", apiEndpoints + "/**").tag("error.outcome", "SERVER_ERROR")
									.errorRatio(
											(s) -> s.name("http.server.requests").tag("uri",
													(uri) -> uri.startsWith(apiEndpoints)),
											(all) -> all.tag("outcome", "SERVER_ERROR"))
									.isLessThan(apiErrorBudget.getValue());
						}).toArray(ServiceLevelObjective[]::new))
				.build();

		for (ServiceLevelObjective slo : registry.getServiceLevelObjectives()) {
			applicationContext.registerBean(this.camelCasedHealthIndicatorNames.name(slo.getName(), Meter.Type.GAUGE),
					HealthContributor.class, () -> toHealthContributor(registry, slo));
		}

		return registry;
	}

	private HealthContributor toHealthContributor(HealthMeterRegistry registry, ServiceLevelObjective slo) {
		if (slo instanceof ServiceLevelObjective.SingleIndicator) {
			final NamingConvention tagConvention = this.camelCasedHealthIndicatorNames;
			return new AbstractHealthIndicator(slo.getFailedMessage()) {
				@Override
				protected void doHealthCheck(Health.Builder builder) {
					ServiceLevelObjective.SingleIndicator singleIndicator = (ServiceLevelObjective.SingleIndicator) slo;
					builder.status(slo.healthy(registry) ? Status.UP : Status.OUT_OF_SERVICE)
							.withDetail("value", singleIndicator.getValueAsString(registry))
							.withDetail("mustBe", singleIndicator.getTestDescription());

					for (Tag tag : slo.getTags()) {
						builder.withDetail(tagConvention.tagKey(tag.getKey()), tag.getValue());
					}

					if (slo.getBaseUnit() != null) {
						builder.withDetail("unit", slo.getBaseUnit());
					}
				}
			};
		}
		else {
			ServiceLevelObjective.MultipleIndicator multipleIndicator = (ServiceLevelObjective.MultipleIndicator) slo;
			Map<String, HealthContributor> objectiveIndicators = Arrays.stream(multipleIndicator.getObjectives())
					.collect(
							Collectors.toMap(
									(indicator) -> this.camelCasedHealthIndicatorNames.name(indicator.getName(),
											Meter.Type.GAUGE),
									(indicator) -> toHealthContributor(registry, indicator)));
			return CompositeHealthContributor.fromMap(objectiveIndicators);
		}
	}

}
