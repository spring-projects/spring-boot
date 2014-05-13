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

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValues;
import org.springframework.boot.bind.RelaxedDataBinder;
import org.springframework.util.ClassUtils;

/**
 * Convenience class for building a {@link DataSource} with common implementations and
 * properties. If Tomcat, HikariCP or Commons DBCP are on the classpath one of them will
 * be selected (in that order with Tomcat first). In the interest of a uniform interface,
 * and so that there can be a fallback to an embedded database if one can be detected on
 * the classpath, only a small set of common configuration properties are supported. To
 * inject additional properties into the result you can downcast it, or use
 * <code>@ConfigurationProperties</code>.
 * 
 * @author Dave Syer
 */
public class DataSourceFactory {

	private static final String[] DATA_SOURCE_TYPE_NAMES = new String[] {
			"org.apache.tomcat.jdbc.pool.DataSource",
			"com.zaxxer.hikari.HikariDataSource",
			"org.apache.commons.dbcp.BasicDataSource" };

	private Class<? extends DataSource> type;

	private ClassLoader classLoader;

	private Map<String, String> properties = new HashMap<String, String>();

	public static DataSourceFactory create() {
		return new DataSourceFactory(null);
	}

	public static DataSourceFactory create(ClassLoader classLoader) {
		return new DataSourceFactory(classLoader);
	}

	public DataSourceFactory(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	public DataSource build() {
		Class<? extends DataSource> type = getType();
		DataSource result = BeanUtils.instantiate(type);
		bind(result);
		return result;
	}

	private void bind(DataSource result) {
		new RelaxedDataBinder(result).bind(getPropertyValues());
	}

	private PropertyValues getPropertyValues() {
		if (getType().getName().contains("Hikari") && this.properties.containsKey("url")) {
			this.properties.put("jdbcUrl", this.properties.get("url"));
			this.properties.remove("url");
		}
		return new MutablePropertyValues(this.properties);
	}

	public DataSourceFactory type(Class<? extends DataSource> type) {
		this.type = type;
		return this;
	}

	public DataSourceFactory url(String url) {
		this.properties.put("url", url);
		return this;
	}

	public DataSourceFactory driverClassName(String driverClassName) {
		this.properties.put("driverClassName", driverClassName);
		return this;
	}

	public DataSourceFactory username(String username) {
		this.properties.put("username", username);
		return this;
	}

	public DataSourceFactory password(String password) {
		this.properties.put("password", password);
		return this;
	}

	public Class<? extends DataSource> findType() {
		if (this.type != null) {
			return this.type;
		}
		for (String name : DATA_SOURCE_TYPE_NAMES) {
			if (ClassUtils.isPresent(name, this.classLoader)) {
				@SuppressWarnings("unchecked")
				Class<DataSource> resolved = (Class<DataSource>) ClassUtils
						.resolveClassName(name, this.classLoader);
				return resolved;
			}
		}
		return null;
	}

	private Class<? extends DataSource> getType() {
		Class<? extends DataSource> type = findType();
		if (type != null) {
			return type;
		}
		throw new IllegalStateException("No supported DataSource type found");
	}

}
