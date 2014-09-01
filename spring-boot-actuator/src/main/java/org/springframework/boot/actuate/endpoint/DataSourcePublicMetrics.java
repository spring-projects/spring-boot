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
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadataProviders;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadata;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadataProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Primary;

/**
 * A {@link PublicMetrics} implementation that provides data source usage statistics.
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
public class DataSourcePublicMetrics implements PublicMetrics {

	private static final String DATASOURCE_SUFFIX = "dataSource";

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private Collection<DataSourcePoolMetadataProvider> providers;

	private final Map<String, DataSourcePoolMetadata> metadataByPrefix = new HashMap<String, DataSourcePoolMetadata>();

	@PostConstruct
	public void initialize() {
		DataSource primaryDataSource = getPrimaryDataSource();
		DataSourcePoolMetadataProvider provider = new DataSourcePoolMetadataProviders(
				this.providers);
		for (Map.Entry<String, DataSource> entry : this.applicationContext
				.getBeansOfType(DataSource.class).entrySet()) {
			String beanName = entry.getKey();
			DataSource bean = entry.getValue();
			String prefix = createPrefix(beanName, bean, bean.equals(primaryDataSource));
			DataSourcePoolMetadata poolMetadata = provider.getDataSourcePoolMetadata(bean);
			if (poolMetadata != null) {
				this.metadataByPrefix.put(prefix, poolMetadata);
			}
		}
	}

	@Override
	public Collection<Metric<?>> metrics() {
		Set<Metric<?>> metrics = new LinkedHashSet<Metric<?>>();
		for (Map.Entry<String, DataSourcePoolMetadata> entry : this.metadataByPrefix
				.entrySet()) {
			String prefix = entry.getKey();
			prefix = (prefix.endsWith(".") ? prefix : prefix + ".");
			DataSourcePoolMetadata dataSourceMetadata = entry.getValue();
			addMetric(metrics, prefix + "active", dataSourceMetadata.getActive());
			addMetric(metrics, prefix + "usage", dataSourceMetadata.getUsage());
		}
		return metrics;
	}

	private <T extends Number> void addMetric(Set<Metric<?>> metrics, String name, T value) {
		if (value != null) {
			metrics.add(new Metric<T>(name, value));
		}
	}

	/**
	 * Create the prefix to use for the metrics to associate with the given
	 * {@link DataSource}.
	 * @param name the name of the data source bean
	 * @param dataSource the data source to configure
	 * @param primary if this data source is the primary data source
	 * @return a prefix for the given data source
	 */
	protected String createPrefix(String name, DataSource dataSource, boolean primary) {
		if (primary) {
			return "datasource.primary";
		}
		if (name.toLowerCase().endsWith(DATASOURCE_SUFFIX.toLowerCase())) {
			name = name.substring(0, name.length() - DATASOURCE_SUFFIX.length());
		}
		return "datasource." + name;
	}

	/**
	 * Attempt to locate the primary {@link DataSource} (i.e. either the only data source
	 * available or the one amongst the candidates marked as {@link Primary}. Return
	 * {@code null} if there no primary data source could be found.
	 */
	private DataSource getPrimaryDataSource() {
		try {
			return this.applicationContext.getBean(DataSource.class);
		}
		catch (NoSuchBeanDefinitionException ex) {
			return null;
		}
	}
}
