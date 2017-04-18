/*
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

package org.springframework.boot.autoconfigure.jdbc.metadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.jdbc.decorator.DecoratedDataSource;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * {@link DataSourcePoolMetadataProvider} that returns specific implementation for a real
 * {@link DataSource} extracted from a {@link DecoratedDataSource}.
 *
 * @author Arthur Gavlyukovskiy
 */
class DecoratedDataSourcePoolMetadataProvider implements DataSourcePoolMetadataProvider, ApplicationContextAware {

	private ApplicationContext applicationContext;
	private DataSourcePoolMetadataProvider provider;

	@PostConstruct
	public void init() {
		Collection<DataSourcePoolMetadataProvider> delegates = this.applicationContext
			.getBeansOfType(DataSourcePoolMetadataProvider.class).values();
		List<DataSourcePoolMetadataProvider> realDataSourceProviders = new ArrayList<DataSourcePoolMetadataProvider>();
		for (DataSourcePoolMetadataProvider delegate : delegates) {
			if (!(delegate instanceof DecoratedDataSourcePoolMetadataProvider)) {
				realDataSourceProviders.add(delegate);
			}
		}
		this.provider = new DataSourcePoolMetadataProviders(realDataSourceProviders);
	}

	/**
	 * Returns implementation for a real {@link DataSource}, returns null if a
	 * {@link DataSourcePoolMetadata} implementation can not be found for the real
	 * {@link DataSource}.
	 *
	 * @param dataSource data source
	 * @return {@link DataSourcePoolMetadata} for a real data source or null
	 */
	@Override
	public DataSourcePoolMetadata getDataSourcePoolMetadata(DataSource dataSource) {
		if (!(dataSource instanceof DecoratedDataSource)) {
			return null;
		}
		DataSource realDataSource = ((DecoratedDataSource) dataSource).getRealDataSource();
		return this.provider.getDataSourcePoolMetadata(realDataSource);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
}
