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

package org.springframework.boot.jdbc;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.BeanUtils;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertyNameAliases;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.util.ClassUtils;

/**
 * Convenience class for building a {@link DataSource} with common implementations and
 * properties. If HikariCP, Tomcat or Commons DBCP are on the classpath one of them will
 * be selected (in that order with Hikari first). In the interest of a uniform interface,
 * and so that there can be a fallback to an embedded database if one can be detected on
 * the classpath, only a small set of common configuration properties are supported. To
 * inject additional properties into the result you can downcast it, or use
 * {@code @ConfigurationProperties}.
 *
 * @param <T> type of DataSource produced by the builder
 * @author Dave Syer
 * @author Madhura Bhave
 * @since 2.0.0
 */
public final class DataSourceBuilder<T extends DataSource> {

	private static final String[] DATA_SOURCE_TYPE_NAMES = new String[] { "com.zaxxer.hikari.HikariDataSource",
			"org.apache.tomcat.jdbc.pool.DataSource", "org.apache.commons.dbcp2.BasicDataSource" };

	private Class<? extends DataSource> type;

	private ClassLoader classLoader;

	private Map<String, String> properties = new HashMap<>();

	public static DataSourceBuilder<?> create() {
		return new DataSourceBuilder<>(null);
	}

	public static DataSourceBuilder<?> create(ClassLoader classLoader) {
		return new DataSourceBuilder<>(classLoader);
	}

	private DataSourceBuilder(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@SuppressWarnings("unchecked")
	public T build() {
		Class<? extends DataSource> type = getType();
		DataSource result = BeanUtils.instantiateClass(type);
		maybeGetDriverClassName();
		bind(result);
		return (T) result;
	}

	private void maybeGetDriverClassName() {
		if (!this.properties.containsKey("driverClassName") && this.properties.containsKey("url")) {
			String url = this.properties.get("url");
			String driverClass = DatabaseDriver.fromJdbcUrl(url).getDriverClassName();
			this.properties.put("driverClassName", driverClass);
		}
	}

	private void bind(DataSource result) {
		ConfigurationPropertySource source = new MapConfigurationPropertySource(this.properties);
		ConfigurationPropertyNameAliases aliases = new ConfigurationPropertyNameAliases();
		aliases.addAliases("url", "jdbc-url");
		aliases.addAliases("username", "user");
		Binder binder = new Binder(source.withAliases(aliases));
		binder.bind(ConfigurationPropertyName.EMPTY, Bindable.ofInstance(result));
	}

	@SuppressWarnings("unchecked")
	public <D extends DataSource> DataSourceBuilder<D> type(Class<D> type) {
		this.type = type;
		return (DataSourceBuilder<D>) this;
	}

	public DataSourceBuilder<T> url(String url) {
		this.properties.put("url", url);
		return this;
	}

	public DataSourceBuilder<T> driverClassName(String driverClassName) {
		this.properties.put("driverClassName", driverClassName);
		return this;
	}

	public DataSourceBuilder<T> username(String username) {
		this.properties.put("username", username);
		return this;
	}

	public DataSourceBuilder<T> password(String password) {
		this.properties.put("password", password);
		return this;
	}

	@SuppressWarnings("unchecked")
	public static Class<? extends DataSource> findType(ClassLoader classLoader) {
		for (String name : DATA_SOURCE_TYPE_NAMES) {
			try {
				return (Class<? extends DataSource>) ClassUtils.forName(name, classLoader);
			}
			catch (Exception ex) {
				// Swallow and continue
			}
		}
		return null;
	}

	private Class<? extends DataSource> getType() {
		Class<? extends DataSource> type = (this.type != null) ? this.type : findType(this.classLoader);
		if (type != null) {
			return type;
		}
		throw new IllegalStateException("No supported DataSource type found");
	}

}
