/*
 * Copyright 2012-2013 the original author or authors.
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

import javax.sql.DataSource;

import org.springframework.beans.BeanUtils;
import org.springframework.boot.bind.PropertySourcesPropertyValues;
import org.springframework.boot.bind.RelaxedDataBinder;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.ClassUtils;

/**
 * @author Dave Syer
 */
public class RelaxedDataSourceFactory {

	private static final String[] DATA_SOURCE_TYPE_NAMES = new String[] {
			"com.zaxxer.hikari.HikariDataSource",
			"org.apache.tomcat.jdbc.pool.DataSource",
			"org.apache.commons.dbcp.BasicDataSource" };

	private Class<? extends DataSource> type;

	private ConfigurableEnvironment environment;

	public static RelaxedDataSourceFactory create(ConfigurableEnvironment environment) {
		return new RelaxedDataSourceFactory(environment);
	}

	public RelaxedDataSourceFactory(ConfigurableEnvironment environment) {
		this.environment = environment;
	}

	public DataSource build(String prefix) {
		Class<? extends DataSource> type = getType();
		DataSource result = BeanUtils.instantiate(type);
		RelaxedDataBinder binder = new RelaxedDataBinder(result, prefix);
		binder.bind(new PropertySourcesPropertyValues(this.environment
				.getPropertySources()));
		return result;
	}

	public RelaxedDataSourceFactory type(Class<? extends DataSource> type) {
		this.type = type;
		return this;
	}

	private Class<? extends DataSource> getType() {
		if (this.type != null) {
			return this.type;
		}
		for (String name : DATA_SOURCE_TYPE_NAMES) {
			if (ClassUtils.isPresent(name, null)) {
				@SuppressWarnings("unchecked")
				Class<DataSource> resolved = (Class<DataSource>) ClassUtils
						.resolveClassName(name, null);
				return resolved;
			}
		}
		throw new IllegalStateException("No supported DataSource type found");
	}

}
