/*
 * Copyright 2012-2020 the original author or authors.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.springframework.beans.BeanUtils;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertyNameAliases;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.util.ClassUtils;

import static java.util.Optional.ofNullable;

/**
 * Convenience class for building a {@link DataSource} with common implementations and
 * properties. If HikariCP, Tomcat, Commons DBCP or Oracle UCP are on the classpath one of
 * them will be selected (in that order with Hikari first). In the interest of a uniform
 * interface, and so that there can be a fallback to an embedded database if one can be
 * detected on the classpath, only a small set of common configuration properties are
 * supported. To inject additional properties into the result you can downcast it, or use
 * {@code @ConfigurationProperties}.
 *
 * @param <T> type of DataSource produced by the builder
 * @author Dave Syer
 * @author Madhura Bhave
 * @author Fabio Grassi
 * @since 2.0.0
 */
public final class DataSourceBuilder<T extends DataSource> {

	private Class<? extends DataSource> type;

	private final DataSourceSettingsResolver settingsResolver;

	private final Map<String, String> properties = new HashMap<>();

	public static DataSourceBuilder<?> create() {
		return new DataSourceBuilder<>(null);
	}

	public static DataSourceBuilder<?> create(ClassLoader classLoader) {
		return new DataSourceBuilder<>(classLoader);
	}

	private DataSourceBuilder(ClassLoader classLoader) {
		this.settingsResolver = new DataSourceSettingsResolver(classLoader);
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
		this.settingsResolver.registerAliases(result, aliases);
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

	public static Class<? extends DataSource> findType(ClassLoader classLoader) {
		return new DataSourceSettingsResolver(classLoader).getPreferredType();
	}

	private Class<? extends DataSource> getType() {
		return Stream.of(this.type, this.settingsResolver.getPreferredType()).filter( Objects::nonNull ).findFirst().orElseThrow(() -> new IllegalStateException("No supported DataSource type found"));
	}

	private static class DataSourceSettings {

		private final Class<? extends DataSource> type;

		private final Consumer<ConfigurationPropertyNameAliases> aliasesCustomizer;

		DataSourceSettings(Class<? extends DataSource> type,
				Consumer<ConfigurationPropertyNameAliases> aliasesCustomizer) {
			this.type = Objects.requireNonNull(type);
			this.aliasesCustomizer = Objects.requireNonNull(aliasesCustomizer);
		}

		DataSourceSettings(Class<? extends DataSource> type) {
			this(type, (aliases) -> {});
		}

		DataSourceSettings(Class<? extends DataSource> type, String propertyName, String propertyAlias) {
			this(type, new Consumer<ConfigurationPropertyNameAliases>() {
				@Override
				public void accept(ConfigurationPropertyNameAliases aliases) {
					aliases.addAliases(Objects.requireNonNull(propertyName), Objects.requireNonNull(propertyAlias));
				}
			});
		}

		Class<? extends DataSource> getType() {
			return this.type.isInterface() ? null : this.type;
		}

		void registerAliases(DataSource candidate, ConfigurationPropertyNameAliases aliases) {
			if (this.type.isInstance(candidate)) {
				this.aliasesCustomizer.accept(aliases);
			}
		}

	}

	private static class DataSourceSettingsResolver {

		private final List<DataSourceSettings> allDataSourceSettings = new ArrayList<>();

		private final Class<? extends DataSource> preferredType;

		DataSourceSettingsResolver(ClassLoader classLoader) {
			createAndAddIfAvailable(classLoader, "com.zaxxer.hikari.HikariDataSource", (type) -> new DataSourceSettings(type, "url", "jdbc-url"));
			createAndAddIfAvailable(classLoader, "org.apache.tomcat.jdbc.pool.DataSource", DataSourceSettings::new);
			createAndAddIfAvailable(classLoader, "org.apache.commons.dbcp2.BasicDataSource", DataSourceSettings::new);
			createAndAddIfAvailable(classLoader, "oracle.ucp.jdbc.PoolDataSourceImpl", (type) -> {
				// Unfortunately Oracle UCP has an import on the Oracle driver itself
				if (ClassUtils.isPresent("oracle.jdbc.OracleConnection", classLoader)) {
					return new DataSourceSettings(type, (aliases) -> {
						aliases.addAliases("username", "user");
						aliases.addAliases("driver-class-name", "connection-factory-class-name");
					});
				}
				return null;
			});

			this.preferredType =  this.allDataSourceSettings.isEmpty() ? null : this.allDataSourceSettings.get(0).getType();

			createAndAddIfAvailable(classLoader, "org.springframework.jdbc.datasource.SimpleDriverDataSource", (type) -> new DataSourceSettings(type, "driver-class-name", "driver-class"));
			createAndAddIfAvailable(classLoader, "oracle.jdbc.datasource.OracleDataSource", (type) -> new DataSourceSettings(type, "username", "user"));
		}

		private void createAndAddIfAvailable(ClassLoader classLoader, String target,
				Function<Class<? extends DataSource>, DataSourceSettings> factory) {
			findDataSourceClass(classLoader, target).ifPresent((dataSourceClass) -> {ofNullable(factory.apply(dataSourceClass)).ifPresent(this.allDataSourceSettings::add);});
		}

		@SuppressWarnings("unchecked")
		private static Optional<Class<? extends DataSource>> findDataSourceClass(ClassLoader classLoader, String target) {
			Class<?> candidateClass = null;
			
			try {
				candidateClass = ClassUtils.forName(target, classLoader);
			} catch (Exception ex) {
				// Swallow and let candidateClass set to null
			}
			
			Class<? extends DataSource> result = null;
			
			if (candidateClass != null && DataSource.class.isAssignableFrom(candidateClass)) {
				result = (Class<? extends DataSource>) candidateClass;
			} 
				
			return ofNullable(result);
		}
		
		Class<? extends DataSource> getPreferredType() {
			return this.preferredType;
		}

		void registerAliases(DataSource result, ConfigurationPropertyNameAliases aliases) {
			this.allDataSourceSettings.forEach((settings) -> settings.registerAliases(result, aliases));
		}

	}

}
