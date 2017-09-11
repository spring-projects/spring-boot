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

import static java.util.Arrays.asList;

import java.util.Collection;

import javax.sql.DataSource;

import org.springframework.boot.actuate.metrics.binder.DataSourceMetrics;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadataProvider;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.ExecutorServiceMetrics;

/**
 * @since 2.0.0
 * @author Jon Schneider
 */
public class SpringMeters {
    /**
     * Record metrics on active connections and connection pool utilization.
     *
     * @param registry          The registry to bind metrics to.
     * @param dataSource        The data source to instrument.
     * @param metadataProviders A list of providers from which the instrumentation can look up information about pool usage.
     * @param name              The name prefix of the metrics.
     * @param tags              Tags to apply to all recorded metrics.
     * @return The instrumented data source, unchanged. The original data source
     * is not wrapped or proxied in any way.
     */
    public static DataSource monitor(MeterRegistry registry,
                                     DataSource dataSource,
                                     Collection<DataSourcePoolMetadataProvider> metadataProviders,
                                     String name,
                                     Iterable<Tag> tags) {
        new DataSourceMetrics(dataSource, metadataProviders, name, tags).bindTo(registry);
        return dataSource;
    }

    /**
     * Record metrics on active connections and connection pool utilization.
     *
     * @param registry          The registry to bind metrics to.
     * @param dataSource        The data source to instrument.
     * @param metadataProviders A list of providers from which the instrumentation can look up information about pool usage.
     * @param name              The name prefix of the metrics
     * @param tags              Tags to apply to all recorded metrics.
     * @return The instrumented data source, unchanged. The original data source
     * is not wrapped or proxied in any way.
     */
    public static DataSource monitor(MeterRegistry registry,
                                     DataSource dataSource,
                                     Collection<DataSourcePoolMetadataProvider> metadataProviders,
                                     String name,
                                     Tag... tags) {
        return monitor(registry, dataSource, metadataProviders, name, asList(tags));
    }

    /**
     * Record metrics on the use of a {@link ThreadPoolTaskExecutor}.
     *
     * @param registry The registry to bind metrics to.
     * @param executor The task executor to instrument.
     * @param name     The name prefix of the metrics.
     * @param tags     Tags to apply to all recorded metrics.
     * @return The instrumented executor, proxied.
     */
    public static ThreadPoolTaskExecutor monitor(MeterRegistry registry, ThreadPoolTaskExecutor executor, String name, Iterable<Tag> tags) {
        ExecutorServiceMetrics.monitor(registry, executor.getThreadPoolExecutor(), name, tags);
        return executor;
    }

    /**
     * Record metrics on the use of a {@link ThreadPoolTaskExecutor}.
     *
     * @param registry The registry to bind metrics to.
     * @param executor The executor to instrument.
     * @param name     The name prefix of the metrics.
     * @param tags     Tags to apply to all recorded metrics.
     * @return The instrumented executor, proxied.
     */
    public static ThreadPoolTaskExecutor monitor(MeterRegistry registry, ThreadPoolTaskExecutor executor, String name, Tag... tags) {
        ExecutorServiceMetrics.monitor(registry, executor.getThreadPoolExecutor(), name, tags);
        return executor;
    }

    /**
     * Record metrics on the use of a {@link ThreadPoolTaskExecutor}.
     *
     * @param registry  The registry to bind metrics to.
     * @param scheduler The task scheduler to instrument.
     * @param name      The name prefix of the metrics.
     * @param tags      Tags to apply to all recorded metrics.
     * @return The instrumented scheduler, proxied.
     */
    public static ThreadPoolTaskScheduler monitor(MeterRegistry registry, ThreadPoolTaskScheduler scheduler, String name, Iterable<Tag> tags) {
        ExecutorServiceMetrics.monitor(registry, scheduler.getScheduledExecutor(), name, tags);
        return scheduler;
    }

    /**
     * Record metrics on the use of a {@link ThreadPoolTaskExecutor}.
     *
     * @param registry  The registry to bind metrics to.
     * @param scheduler The scheduler to instrument.
     * @param name      The name prefix of the metrics.
     * @param tags      Tags to apply to all recorded metrics.
     * @return The instrumented scheduler, proxied.
     */
    public static ThreadPoolTaskScheduler monitor(MeterRegistry registry, ThreadPoolTaskScheduler scheduler, String name, Tag... tags) {
        ExecutorServiceMetrics.monitor(registry, scheduler.getScheduledExecutor(), name, tags);
        return scheduler;
    }
}
