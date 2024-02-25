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

package org.springframework.boot.actuate.metrics.jdbc;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

import javax.sql.DataSource;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;

import org.springframework.boot.jdbc.metadata.CompositeDataSourcePoolMetadataProvider;
import org.springframework.boot.jdbc.metadata.DataSourcePoolMetadata;
import org.springframework.boot.jdbc.metadata.DataSourcePoolMetadataProvider;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * A {@link MeterBinder} for a {@link DataSource}.
 *
 * @author Jon Schneider
 * @author Phillip Webb
 * @since 2.0.0
 */
public class DataSourcePoolMetrics implements MeterBinder {

	private final DataSource dataSource;

	private final CachingDataSourcePoolMetadataProvider metadataProvider;

	private final Iterable<Tag> tags;

	/**
     * Constructs a new DataSourcePoolMetrics object with the specified parameters.
     * 
     * @param dataSource the DataSource object to monitor
     * @param metadataProviders the collection of DataSourcePoolMetadataProvider objects to provide metadata for the DataSource
     * @param dataSourceName the name of the DataSource
     * @param tags the iterable collection of tags to associate with the metrics
     */
    public DataSourcePoolMetrics(DataSource dataSource, Collection<DataSourcePoolMetadataProvider> metadataProviders,
			String dataSourceName, Iterable<Tag> tags) {
		this(dataSource, new CompositeDataSourcePoolMetadataProvider(metadataProviders), dataSourceName, tags);
	}

	/**
     * Constructs a new DataSourcePoolMetrics object with the specified DataSource, DataSourcePoolMetadataProvider,
     * name, and tags.
     *
     * @param dataSource the DataSource to monitor
     * @param metadataProvider the DataSourcePoolMetadataProvider to retrieve metadata from
     * @param name the name of the DataSourcePoolMetrics object
     * @param tags additional tags to associate with the metrics
     * @throws IllegalArgumentException if either dataSource or metadataProvider is null
     */
    public DataSourcePoolMetrics(DataSource dataSource, DataSourcePoolMetadataProvider metadataProvider, String name,
			Iterable<Tag> tags) {
		Assert.notNull(dataSource, "DataSource must not be null");
		Assert.notNull(metadataProvider, "MetadataProvider must not be null");
		this.dataSource = dataSource;
		this.metadataProvider = new CachingDataSourcePoolMetadataProvider(metadataProvider);
		this.tags = Tags.concat(tags, "name", name);
	}

	/**
     * Binds the DataSourcePoolMetrics to the given MeterRegistry.
     *
     * @param registry the MeterRegistry to bind the metrics to
     */
    @Override
	public void bindTo(MeterRegistry registry) {
		if (this.metadataProvider.getDataSourcePoolMetadata(this.dataSource) != null) {
			bindPoolMetadata(registry, "active",
					"Current number of active connections that have been allocated from the data source.",
					DataSourcePoolMetadata::getActive);
			bindPoolMetadata(registry, "idle", "Number of established but idle connections.",
					DataSourcePoolMetadata::getIdle);
			bindPoolMetadata(registry, "max",
					"Maximum number of active connections that can be allocated at the same time.",
					DataSourcePoolMetadata::getMax);
			bindPoolMetadata(registry, "min", "Minimum number of idle connections in the pool.",
					DataSourcePoolMetadata::getMin);
		}
	}

	/**
     * Binds the pool metadata to the specified metric in the given MeterRegistry.
     * 
     * @param registry the MeterRegistry to bind the metric to
     * @param metricName the name of the metric
     * @param description the description of the metric
     * @param function the function to extract the pool metadata value from the DataSourcePoolMetadata
     * @param <N> the type of the pool metadata value, must extend Number
     */
    private <N extends Number> void bindPoolMetadata(MeterRegistry registry, String metricName, String description,
			Function<DataSourcePoolMetadata, N> function) {
		bindDataSource(registry, metricName, description, this.metadataProvider.getValueFunction(function));
	}

	/**
     * Binds a data source to the given MeterRegistry by creating a Gauge metric with the provided metricName, description, and function.
     * The function is used to extract a numeric value from the data source.
     * If the function returns a non-null value for the data source, the Gauge metric is registered with the MeterRegistry.
     * 
     * @param registry The MeterRegistry to bind the data source to.
     * @param metricName The name of the metric to be created.
     * @param description The description of the metric.
     * @param function The function to extract a numeric value from the data source.
     * @param <N> The type of the numeric value extracted from the data source.
     */
    private <N extends Number> void bindDataSource(MeterRegistry registry, String metricName, String description,
			Function<DataSource, N> function) {
		if (function.apply(this.dataSource) != null) {
			Gauge.builder("jdbc.connections." + metricName, this.dataSource, (m) -> function.apply(m).doubleValue())
				.tags(this.tags)
				.description(description)
				.register(registry);
		}
	}

	/**
     * CachingDataSourcePoolMetadataProvider class.
     */
    private static class CachingDataSourcePoolMetadataProvider implements DataSourcePoolMetadataProvider {

		private static final Map<DataSource, DataSourcePoolMetadata> cache = new ConcurrentReferenceHashMap<>();

		private final DataSourcePoolMetadataProvider metadataProvider;

		/**
         * Constructs a new CachingDataSourcePoolMetadataProvider with the specified DataSourcePoolMetadataProvider.
         * 
         * @param metadataProvider the DataSourcePoolMetadataProvider to be used for retrieving metadata
         */
        CachingDataSourcePoolMetadataProvider(DataSourcePoolMetadataProvider metadataProvider) {
			this.metadataProvider = metadataProvider;
		}

		/**
         * Returns a function that extracts a value of type N from a given DataSource.
         * The function is obtained by applying the provided function to the DataSourcePoolMetadata
         * obtained from the given DataSource.
         *
         * @param function the function to be applied to the DataSourcePoolMetadata
         * @param <N> the type of value to be extracted
         * @return a function that extracts a value of type N from a given DataSource
         */
        <N extends Number> Function<DataSource, N> getValueFunction(Function<DataSourcePoolMetadata, N> function) {
			return (dataSource) -> function.apply(getDataSourcePoolMetadata(dataSource));
		}

		/**
         * Retrieves the pool metadata for the given data source.
         * 
         * @param dataSource the data source for which to retrieve the pool metadata
         * @return the pool metadata for the given data source
         */
        @Override
		public DataSourcePoolMetadata getDataSourcePoolMetadata(DataSource dataSource) {
			return cache.computeIfAbsent(dataSource,
					(key) -> this.metadataProvider.getDataSourcePoolMetadata(dataSource));
		}

	}

}
