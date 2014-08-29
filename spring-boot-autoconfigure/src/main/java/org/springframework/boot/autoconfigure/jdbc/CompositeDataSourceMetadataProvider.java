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

import javax.sql.DataSource;

/**
 * A {@link DataSourceMetadataProvider} implementation that returns the first
 * {@link DataSourceMetadata} that is found by one of its delegate.
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
public class CompositeDataSourceMetadataProvider implements DataSourceMetadataProvider {

	private final Collection<DataSourceMetadataProvider> providers;

	/**
	 * Create an instance with an initial collection of delegates to use.
	 */
	public CompositeDataSourceMetadataProvider(Collection<DataSourceMetadataProvider> providers) {
		this.providers = providers;
	}

	/**
	 * Create an instance with no delegate.
	 */
	public CompositeDataSourceMetadataProvider() {
		this(new ArrayList<DataSourceMetadataProvider>());
	}

	@Override
	public DataSourceMetadata getDataSourceMetadata(DataSource dataSource) {
		for (DataSourceMetadataProvider provider : providers) {
			DataSourceMetadata dataSourceMetadata = provider.getDataSourceMetadata(dataSource);
			if (dataSourceMetadata != null) {
				return dataSourceMetadata;
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
