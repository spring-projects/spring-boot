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

package org.springframework.boot.autoconfigure.jdbc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.sql.DataSource;

/**
 * A {@link DataSourceMetadataProvider} implementation that returns the first
 * {@link DataSourceMetadata} that is found by one of its delegate.
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
public class CompositeDataSourceMetadataProvider implements DataSourceMetadataProvider {

	private final List<DataSourceMetadataProvider> providers;

	/**
	 * Create a {@link CompositeDataSourceMetadataProvider} instance with no delegate.
	 */
	public CompositeDataSourceMetadataProvider() {
		this(new ArrayList<DataSourceMetadataProvider>());
	}

	/**
	 * Create a {@link CompositeDataSourceMetadataProvider} instance with an initial
	 * collection of delegates to use.
	 */
	public CompositeDataSourceMetadataProvider(
			Collection<? extends DataSourceMetadataProvider> providers) {
		this.providers = new ArrayList<DataSourceMetadataProvider>(providers);
	}

	@Override
	public DataSourceMetadata getDataSourceMetadata(DataSource dataSource) {
		for (DataSourceMetadataProvider provider : this.providers) {
			DataSourceMetadata metadata = provider.getDataSourceMetadata(dataSource);
			if (metadata != null) {
				return metadata;
			}
		}
		return null;
	}

	/**
	 * Add a {@link DataSourceMetadataProvider} delegate to the list.
	 */
	public void addDataSourceMetadataProvider(DataSourceMetadataProvider provider) {
		this.providers.add(provider);
	}

}
