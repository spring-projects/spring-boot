/**
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
package org.springframework.boot.actuate.metrics;

import java.util.Collection;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.metrics.binder.SpringIntegrationMetrics;
import org.springframework.boot.actuate.metrics.export.MetricsExporter;
import org.springframework.boot.actuate.metrics.export.atlas.AtlasExportConfiguration;
import org.springframework.boot.actuate.metrics.export.datadog.DatadogExportConfiguration;
import org.springframework.boot.actuate.metrics.export.ganglia.GangliaExportConfiguration;
import org.springframework.boot.actuate.metrics.export.graphite.GraphiteExportConfiguration;
import org.springframework.boot.actuate.metrics.export.influx.InfluxExportConfiguration;
import org.springframework.boot.actuate.metrics.export.jmx.JmxExportConfiguration;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusExportConfiguration;
import org.springframework.boot.actuate.metrics.export.simple.SimpleExportConfiguration;
import org.springframework.boot.actuate.metrics.scheduling.ScheduledMethodMetrics;
import org.springframework.boot.actuate.metrics.web.MetricsRestTemplateConfiguration;
import org.springframework.boot.actuate.metrics.web.MetricsServletRequestConfiguration;
import org.springframework.boot.actuate.metrics.web.MetricsWebfluxRequestConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.support.management.IntegrationManagementConfigurer;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;

/**
 * Metrics configuration for Spring 4/Boot 1.x
 *
 * @since 2.0.0
 * @author Jon Schneider
 */
@Configuration
@EnableConfigurationProperties(MetricsConfigurationProperties.class)
@Import({
    RecommendedMeterBinders.class,
    MetricsServletRequestConfiguration.class,
    MetricsWebfluxRequestConfiguration.class,
    MetricsRestTemplateConfiguration.class,

    // supported monitoring systems
    AtlasExportConfiguration.class,
    DatadogExportConfiguration.class,
    GangliaExportConfiguration.class,
    GraphiteExportConfiguration.class,
    InfluxExportConfiguration.class,
    JmxExportConfiguration.class,
    PrometheusExportConfiguration.class,
    SimpleExportConfiguration.class,
})
class MetricsConfiguration {
    @ConditionalOnMissingBean(MeterRegistry.class)
    @Bean
    public CompositeMeterRegistry compositeMeterRegistry(ObjectProvider<Collection<MetricsExporter>> exportersProvider) {
        CompositeMeterRegistry composite = new CompositeMeterRegistry();

        if (exportersProvider.getIfAvailable() != null) {
            exportersProvider.getIfAvailable().forEach(exporter -> composite.add(exporter.registry()));
        }

        return composite;
    }

    @Configuration
    static class MeterRegistryConfigurationSupport {
        public MeterRegistryConfigurationSupport(MeterRegistry registry,
                                                 MetricsConfigurationProperties config,
                                                 ObjectProvider<Collection<MeterBinder>> binders,
                                                 ObjectProvider<Collection<MeterRegistryConfigurer>> registryConfigurers) {
            if (registryConfigurers.getIfAvailable() != null) {
                registryConfigurers.getIfAvailable().forEach(conf -> conf.configureRegistry(registry));
            }

            if (binders.getIfAvailable() != null) {
                binders.getIfAvailable().forEach(binder -> binder.bindTo(registry));
            }

            if (config.getUseGlobalRegistry()) {
                Metrics.addRegistry(registry);
            }
        }
    }

    /**
     * If AOP is not enabled, scheduled interception will not work.
     */
    @Bean
    @ConditionalOnClass(name = "org.aspectj.lang.ProceedingJoinPoint")
    @ConditionalOnProperty(value = "spring.aop.enabled", havingValue = "true", matchIfMissing = true)
    public ScheduledMethodMetrics metricsSchedulingAspect(MeterRegistry registry) {
        return new ScheduledMethodMetrics(registry);
    }

    @Configuration
    @ConditionalOnClass(name = "org.springframework.integration.config.EnableIntegrationManagement")
    static class MetricsIntegrationConfiguration {

        @Bean(name = IntegrationManagementConfigurer.MANAGEMENT_CONFIGURER_NAME)
        @ConditionalOnMissingBean(value = IntegrationManagementConfigurer.class, name = IntegrationManagementConfigurer.MANAGEMENT_CONFIGURER_NAME, search = SearchStrategy.CURRENT)
        public IntegrationManagementConfigurer managementConfigurer() {
            IntegrationManagementConfigurer configurer = new IntegrationManagementConfigurer();
            configurer.setDefaultCountsEnabled(true);
            configurer.setDefaultStatsEnabled(true);
            return configurer;
        }

        @Bean
        public SpringIntegrationMetrics springIntegrationMetrics(IntegrationManagementConfigurer configurer) {
            return new SpringIntegrationMetrics(configurer);
        }
    }
}
