/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.actuate.endpoint;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.jdbc.CompositeDataSourceMetadataProvider;
import org.springframework.boot.actuate.metrics.jdbc.DataSourceMetadata;
import org.springframework.boot.actuate.metrics.jdbc.DataSourceMetadataProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Primary;

/**
 * A {@link PublicMetrics} implementation that provides data source usage
 * statistics.
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
public class DataSourcePublicMetrics implements PublicMetrics {

	private static final String DATASOURCE_SUFFIX = "dataSource";

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private Collection<DataSourceMetadataProvider> dataSourceMetadataProviders;

	private final Map<String, DataSourceMetadata> dataSourceMetadataByPrefix
			= new HashMap<String, DataSourceMetadata>();

	@PostConstruct
	public void initialize() {
		Map<String, DataSource> dataSources = this.applicationContext.getBeansOfType(DataSource.class);
		DataSource primaryDataSource = getPrimaryDataSource();


		DataSourceMetadataProvider provider = new CompositeDataSourceMetadataProvider(this.dataSourceMetadataProviders);
		for (Map.Entry<String, DataSource> entry : dataSources.entrySet()) {
			String prefix = createPrefix(entry.getKey(), entry.getValue(), entry.getValue().equals(primaryDataSource));
			DataSourceMetadata dataSourceMetadata = provider.getDataSourceMetadata(entry.getValue());
			if (dataSourceMetadata != null) {
				dataSourceMetadataByPrefix.put(prefix, dataSourceMetadata);
			}
		}
	}

	@Override
	public Collection<Metric<?>> metrics() {
		Collection<Metric<?>> result = new LinkedHashSet<Metric<?>>();
		for (Map.Entry<String, DataSourceMetadata> entry : dataSourceMetadataByPrefix.entrySet()) {
			String prefix = entry.getKey();
			// Make sure the prefix ends with a dot
			if (!prefix.endsWith(".")) {
				prefix = prefix + ".";
			}
			DataSourceMetadata dataSourceMetadata = entry.getValue();
			Integer poolSize = dataSourceMetadata.getPoolSize();
			if (poolSize != null) {
				result.add(new Metric<Integer>(prefix + "active", poolSize));
			}
			Float poolUsage = dataSourceMetadata.getPoolUsage();
			if (poolUsage != null) {
				result.add(new Metric<Float>(prefix + "usage", poolUsage));
			}
		}
		return result;
	}

	/**
	 * Create the prefix to use for the metrics to associate with the given {@link DataSource}.
	 * @param dataSourceName the name of the data source bean
	 * @param dataSource the data source to configure
	 * @param primary if this data source is the primary data source
	 * @return a prefix for the given data source
	 */
	protected String createPrefix(String dataSourceName, DataSource dataSource, boolean primary) {
		StringBuilder sb = new StringBuilder("datasource.");
		if (primary) {
			sb.append("primary");
		}
		else if (endWithDataSource(dataSourceName)) { // Strip the data source part out of the name
			sb.append(dataSourceName.substring(0, dataSourceName.length() - DATASOURCE_SUFFIX.length()));
		}
		else {
			sb.append(dataSourceName);
		}
		return sb.toString();
	}

	/**
	 * Specify if the given value ends with {@value #DATASOURCE_SUFFIX}.
	 */
	protected boolean endWithDataSource(String value) {
		int suffixLength = DATASOURCE_SUFFIX.length();
		int valueLength = value.length();
		if (valueLength > suffixLength) {
			String suffix = value.substring(valueLength - suffixLength, valueLength);
			return suffix.equalsIgnoreCase(DATASOURCE_SUFFIX);
		}
		return false;
	}

	/**
	 * Attempt to locate the primary {@link DataSource} (i.e. either the only data source
	 * available or the one amongst the candidates marked as {@link Primary}. Return
	 * {@code null} if there no primary data source could be found.
	 */
	private DataSource getPrimaryDataSource() {
		try {
			return applicationContext.getBean(DataSource.class);
		}
		catch (NoSuchBeanDefinitionException e) {
			return null;
		}
	}
}
