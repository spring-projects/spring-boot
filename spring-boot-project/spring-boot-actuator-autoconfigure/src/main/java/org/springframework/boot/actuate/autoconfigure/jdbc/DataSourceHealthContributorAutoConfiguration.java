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

package org.springframework.boot.actuate.autoconfigure.jdbc;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.CompositeHealthContributor;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.NamedContributor;
import org.springframework.boot.actuate.jdbc.DataSourceHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.metadata.CompositeDataSourcePoolMetadataProvider;
import org.springframework.boot.jdbc.metadata.DataSourcePoolMetadata;
import org.springframework.boot.jdbc.metadata.DataSourcePoolMetadataProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.util.Assert;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link DataSourceHealthIndicator}.
 *
 * @author Dave Syer
 * @author Christian Dupuis
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Arthur Kalimullin
 * @author Julio Gomez
 * @author Safeer Ansari
 * @since 2.0.0
 */
@AutoConfiguration(after = DataSourceAutoConfiguration.class)
@ConditionalOnClass({ JdbcTemplate.class, AbstractRoutingDataSource.class })
@ConditionalOnBean(DataSource.class)
@ConditionalOnEnabledHealthIndicator("db")
@EnableConfigurationProperties(DataSourceHealthIndicatorProperties.class)
public class DataSourceHealthContributorAutoConfiguration implements InitializingBean {

	private final Collection<DataSourcePoolMetadataProvider> metadataProviders;

	private DataSourcePoolMetadataProvider poolMetadataProvider;

	/**
     * Constructs a new DataSourceHealthContributorAutoConfiguration object with the given metadataProviders.
     * 
     * @param metadataProviders the ObjectProvider of DataSourcePoolMetadataProvider instances
     */
    public DataSourceHealthContributorAutoConfiguration(
			ObjectProvider<DataSourcePoolMetadataProvider> metadataProviders) {
		this.metadataProviders = metadataProviders.orderedStream().toList();
	}

	/**
     * Sets up the {@link CompositeDataSourcePoolMetadataProvider} by initializing the {@link DataSourcePoolMetadataProvider}
     * with the given list of metadata providers.
     * 
     * This method is called after all the properties have been set, allowing for any necessary initialization.
     */
    @Override
	public void afterPropertiesSet() {
		this.poolMetadataProvider = new CompositeDataSourcePoolMetadataProvider(this.metadataProviders);
	}

	/**
     * Creates a {@link HealthContributor} for the database health check.
     * This method is conditional on the absence of beans with names "dbHealthIndicator" and "dbHealthContributor".
     * 
     * @param dataSources                        a map of data sources
     * @param dataSourceHealthIndicatorProperties the properties for the data source health indicator
     * @return the created {@link HealthContributor} for the database health check
     */
    @Bean
	@ConditionalOnMissingBean(name = { "dbHealthIndicator", "dbHealthContributor" })
	public HealthContributor dbHealthContributor(Map<String, DataSource> dataSources,
			DataSourceHealthIndicatorProperties dataSourceHealthIndicatorProperties) {
		if (dataSourceHealthIndicatorProperties.isIgnoreRoutingDataSources()) {
			Map<String, DataSource> filteredDatasources = dataSources.entrySet()
				.stream()
				.filter((e) -> !(e.getValue() instanceof AbstractRoutingDataSource))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
			return createContributor(filteredDatasources);
		}
		return createContributor(dataSources);
	}

	/**
     * Creates a HealthContributor based on the provided beans.
     * 
     * @param beans a map of beans with their corresponding names as keys
     * @return a HealthContributor object
     * @throws IllegalArgumentException if the beans map is empty
     */
    private HealthContributor createContributor(Map<String, DataSource> beans) {
		Assert.notEmpty(beans, "Beans must not be empty");
		if (beans.size() == 1) {
			return createContributor(beans.values().iterator().next());
		}
		return CompositeHealthContributor.fromMap(beans, this::createContributor);
	}

	/**
     * Creates a HealthContributor based on the provided DataSource source.
     * If the source is an instance of AbstractRoutingDataSource, a RoutingDataSourceHealthContributor is created
     * using the provided routingDataSource and createContributor method.
     * Otherwise, a DataSourceHealthIndicator is created using the provided source and validation query.
     *
     * @param source the DataSource source to create the HealthContributor from
     * @return the created HealthContributor
     */
    private HealthContributor createContributor(DataSource source) {
		if (source instanceof AbstractRoutingDataSource routingDataSource) {
			return new RoutingDataSourceHealthContributor(routingDataSource, this::createContributor);
		}
		return new DataSourceHealthIndicator(source, getValidationQuery(source));
	}

	/**
     * Retrieves the validation query for the given DataSource.
     * 
     * @param source the DataSource to retrieve the validation query from
     * @return the validation query, or null if not available
     */
    private String getValidationQuery(DataSource source) {
		DataSourcePoolMetadata poolMetadata = this.poolMetadataProvider.getDataSourcePoolMetadata(source);
		return (poolMetadata != null) ? poolMetadata.getValidationQuery() : null;
	}

	/**
	 * {@link CompositeHealthContributor} used for {@link AbstractRoutingDataSource} beans
	 * where the overall health is composed of a {@link DataSourceHealthIndicator} for
	 * each routed datasource.
	 */
	static class RoutingDataSourceHealthContributor implements CompositeHealthContributor {

		private final CompositeHealthContributor delegate;

		private static final String UNNAMED_DATASOURCE_KEY = "unnamed";

		/**
         * Constructs a new RoutingDataSourceHealthContributor with the given AbstractRoutingDataSource and contributorFunction.
         * 
         * @param routingDataSource the AbstractRoutingDataSource used to retrieve the resolved data sources
         * @param contributorFunction the function used to create the HealthContributor for each data source
         */
        RoutingDataSourceHealthContributor(AbstractRoutingDataSource routingDataSource,
				Function<DataSource, HealthContributor> contributorFunction) {
			Map<String, DataSource> routedDataSources = routingDataSource.getResolvedDataSources()
				.entrySet()
				.stream()
				.collect(Collectors.toMap((e) -> Objects.toString(e.getKey(), UNNAMED_DATASOURCE_KEY),
						Map.Entry::getValue));
			this.delegate = CompositeHealthContributor.fromMap(routedDataSources, contributorFunction);
		}

		/**
         * Returns the HealthContributor with the specified name.
         *
         * @param name the name of the HealthContributor to retrieve
         * @return the HealthContributor with the specified name
         */
        @Override
		public HealthContributor getContributor(String name) {
			return this.delegate.getContributor(name);
		}

		/**
         * Returns an iterator over the elements in this RoutingDataSourceHealthContributor.
         *
         * @return an iterator over the elements in this RoutingDataSourceHealthContributor
         */
        @Override
		public Iterator<NamedContributor<HealthContributor>> iterator() {
			return this.delegate.iterator();
		}

	}

}
