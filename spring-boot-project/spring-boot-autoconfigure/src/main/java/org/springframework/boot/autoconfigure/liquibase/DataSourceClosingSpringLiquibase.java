/*
 * Copyright 2012-2018 the original author or authors.
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
public class DataSourceClosingSpringLiquibase extends SpringLiquibase
		implements DisposableBean {

	private volatile boolean closeDataSourceOnceMigrated = true;

	public void setCloseDataSourceOnceMigrated(boolean closeDataSourceOnceMigrated) {
		this.closeDataSourceOnceMigrated = closeDataSourceOnceMigrated;
	}

	@Override
	public void afterPropertiesSet() throws LiquibaseException {
		super.afterPropertiesSet();
		if (this.closeDataSourceOnceMigrated) {
			closeDataSource();
		}
	}

	private void closeDataSource() {
		Class<?> dataSourceClass = getDataSource().getClass();
		Method closeMethod = ReflectionUtils.findMethod(dataSourceClass, "close");
		if (closeMethod != null) {
			ReflectionUtils.invokeMethod(closeMethod, getDataSource());
		}
	}

	@Override
	public void destroy() throws Exception {
		if (!this.closeDataSourceOnceMigrated) {
			closeDataSource();
		}
	}

}
