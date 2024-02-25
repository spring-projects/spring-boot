/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.liquibase;

import java.lang.reflect.Method;

import javax.sql.DataSource;

import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.ReflectionUtils;

/**
 * A custom {@link SpringLiquibase} extension that closes the underlying
 * {@link DataSource} once the database has been migrated.
 *
 * @author Andy Wilkinson
 * @since 2.0.6
 */
public class DataSourceClosingSpringLiquibase extends SpringLiquibase implements DisposableBean {

	private volatile boolean closeDataSourceOnceMigrated = true;

	/**
	 * Sets the flag indicating whether the data source should be closed once the
	 * migration is completed.
	 * @param closeDataSourceOnceMigrated true if the data source should be closed, false
	 * otherwise
	 */
	public void setCloseDataSourceOnceMigrated(boolean closeDataSourceOnceMigrated) {
		this.closeDataSourceOnceMigrated = closeDataSourceOnceMigrated;
	}

	/**
	 * This method is called after all properties have been set and initializes the
	 * DataSourceClosingSpringLiquibase. It calls the super.afterPropertiesSet() method
	 * and then checks if the closeDataSourceOnceMigrated flag is set to true. If it is,
	 * it calls the closeDataSource() method to close the data source.
	 * @throws LiquibaseException if there is an error during the initialization process.
	 */
	@Override
	public void afterPropertiesSet() throws LiquibaseException {
		super.afterPropertiesSet();
		if (this.closeDataSourceOnceMigrated) {
			closeDataSource();
		}
	}

	/**
	 * Closes the data source.
	 *
	 * This method uses reflection to find and invoke the "close" method of the data
	 * source object. If the "close" method is found, it is invoked to close the data
	 * source.
	 * @throws NullPointerException if the data source is null
	 * @throws RuntimeException if an error occurs while closing the data source
	 */
	private void closeDataSource() {
		Class<?> dataSourceClass = getDataSource().getClass();
		Method closeMethod = ReflectionUtils.findMethod(dataSourceClass, "close");
		if (closeMethod != null) {
			ReflectionUtils.invokeMethod(closeMethod, getDataSource());
		}
	}

	/**
	 * This method is called when the bean is being destroyed. It closes the data source
	 * if it has not been closed already.
	 * @throws Exception if an error occurs while closing the data source
	 */
	@Override
	public void destroy() throws Exception {
		if (!this.closeDataSourceOnceMigrated) {
			closeDataSource();
		}
	}

}
