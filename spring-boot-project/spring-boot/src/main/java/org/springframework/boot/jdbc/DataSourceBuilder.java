/*
 * Copyright 2012-2024 the original author or authors.
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

import java.beans.PropertyVetoException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import javax.sql.DataSource;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.zaxxer.hikari.HikariDataSource;
import oracle.jdbc.datasource.OracleDataSource;
import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceImpl;
import org.apache.commons.dbcp2.BasicDataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.postgresql.ds.PGSimpleDataSource;

import org.springframework.beans.BeanUtils;
import org.springframework.core.ResolvableType;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Convenience class for building a {@link DataSource}. Provides a limited subset of the
 * properties supported by a typical {@link DataSource} as well as detection logic to pick
 * the most suitable pooling {@link DataSource} implementation.
 * <p>
 * The following pooling {@link DataSource} implementations are supported by this builder.
 * When no {@link #type(Class) type} has been explicitly set, the first available pool
 * implementation will be picked:
 * <ul>
 * <li>Hikari ({@code com.zaxxer.hikari.HikariDataSource})</li>
 * <li>Tomcat JDBC Pool ({@code org.apache.tomcat.jdbc.pool.DataSource})</li>
 * <li>Apache DBCP2 ({@code org.apache.commons.dbcp2.BasicDataSource})</li>
 * <li>Oracle UCP ({@code oracle.ucp.jdbc.PoolDataSourceImpl})</li>
 * </ul>
 * <p>
 * The following non-pooling {@link DataSource} implementations can be used when
 * explicitly set as a {@link #type(Class) type}:
 * <ul>
 * <li>Spring's {@code SimpleDriverDataSource}
 * ({@code org.springframework.jdbc.datasource.SimpleDriverDataSource})</li>
 * <li>Oracle ({@code oracle.jdbc.datasource.OracleDataSource})</li>
 * <li>H2 ({@code org.h2.jdbcx.JdbcDataSource})</li>
 * <li>Postgres ({@code org.postgresql.ds.PGSimpleDataSource})</li>
 * <li>Any {@code DataSource} implementation with appropriately named methods</li>
 * </ul>
 * <p>
 * This class is commonly used in an {@code @Bean} method and often combined with
 * {@code @ConfigurationProperties}.
 *
 * @param <T> the {@link DataSource} type being built
 * @author Dave Syer
 * @author Madhura Bhave
 * @author Fabio Grassi
 * @author Phillip Webb
 * @since 2.0.0
 * @see #create()
 * @see #create(ClassLoader)
 * @see #derivedFrom(DataSource)
 */
public final class DataSourceBuilder<T extends DataSource> {

	private final ClassLoader classLoader;

	private final Map<DataSourceProperty, String> values = new HashMap<>();

	private Class<T> type;

	private final DataSource deriveFrom;

	/**
	 * Constructs a new DataSourceBuilder with the specified class loader.
	 * @param classLoader the class loader to be used for loading classes and resources
	 */
	private DataSourceBuilder(ClassLoader classLoader) {
		this.classLoader = classLoader;
		this.deriveFrom = null;
	}

	/**
	 * Constructs a new instance of DataSourceBuilder using the provided DataSource as a
	 * template.
	 * @param deriveFrom the DataSource to derive from (must not be null)
	 * @throws IllegalArgumentException if the deriveFrom parameter is null
	 */
	@SuppressWarnings("unchecked")
	private DataSourceBuilder(T deriveFrom) {
		Assert.notNull(deriveFrom, "DataSource must not be null");
		this.classLoader = deriveFrom.getClass().getClassLoader();
		this.type = (Class<T>) deriveFrom.getClass();
		this.deriveFrom = deriveFrom;
	}

	/**
	 * Set the {@link DataSource} type that should be built.
	 * @param <D> the datasource type
	 * @param type the datasource type
	 * @return this builder
	 */
	@SuppressWarnings("unchecked")
	public <D extends DataSource> DataSourceBuilder<D> type(Class<D> type) {
		this.type = (Class<T>) type;
		return (DataSourceBuilder<D>) this;
	}

	/**
	 * Set the URL that should be used when building the datasource.
	 * @param url the JDBC url
	 * @return this builder
	 */
	public DataSourceBuilder<T> url(String url) {
		set(DataSourceProperty.URL, url);
		return this;
	}

	/**
	 * Set the driver class name that should be used when building the datasource.
	 * @param driverClassName the driver class name
	 * @return this builder
	 */
	public DataSourceBuilder<T> driverClassName(String driverClassName) {
		set(DataSourceProperty.DRIVER_CLASS_NAME, driverClassName);
		return this;
	}

	/**
	 * Set the username that should be used when building the datasource.
	 * @param username the user name
	 * @return this builder
	 */
	public DataSourceBuilder<T> username(String username) {
		set(DataSourceProperty.USERNAME, username);
		return this;
	}

	/**
	 * Set the password that should be used when building the datasource.
	 * @param password the password
	 * @return this builder
	 */
	public DataSourceBuilder<T> password(String password) {
		set(DataSourceProperty.PASSWORD, password);
		return this;
	}

	/**
	 * Sets the value for the specified DataSourceProperty.
	 * @param property the DataSourceProperty to set the value for
	 * @param value the value to set for the specified DataSourceProperty
	 */
	private void set(DataSourceProperty property, String value) {
		this.values.put(property, value);
	}

	/**
	 * Return a newly built {@link DataSource} instance.
	 * @return the built datasource
	 */
	public T build() {
		DataSourceProperties<T> properties = DataSourceProperties.forType(this.classLoader, this.type);
		DataSourceProperties<DataSource> deriveFromProperties = getDeriveFromProperties();
		Class<? extends T> instanceType = (this.type != null) ? this.type : properties.getDataSourceInstanceType();
		T dataSource = BeanUtils.instantiateClass(instanceType);
		Set<DataSourceProperty> applied = new HashSet<>();
		for (DataSourceProperty property : DataSourceProperty.values()) {
			String value = this.values.get(property);
			if (value == null && deriveFromProperties != null && properties.canSet(property)) {
				value = deriveFromProperties.get(this.deriveFrom, property);
			}
			if (value != null) {
				properties.set(dataSource, property, value);
				applied.add(property);
			}
		}
		if (!applied.contains(DataSourceProperty.DRIVER_CLASS_NAME)
				&& properties.canSet(DataSourceProperty.DRIVER_CLASS_NAME)
				&& this.values.containsKey(DataSourceProperty.URL)) {
			String url = this.values.get(DataSourceProperty.URL);
			DatabaseDriver driver = DatabaseDriver.fromJdbcUrl(url);
			properties.set(dataSource, DataSourceProperty.DRIVER_CLASS_NAME, driver.getDriverClassName());
		}
		return dataSource;
	}

	/**
	 * Retrieves the DataSourceProperties object derived from the specified deriveFrom
	 * object.
	 * @return The DataSourceProperties object derived from the deriveFrom object, or null
	 * if deriveFrom is null.
	 */
	@SuppressWarnings("unchecked")
	private DataSourceProperties<DataSource> getDeriveFromProperties() {
		if (this.deriveFrom == null) {
			return null;
		}
		return DataSourceProperties.forType(this.classLoader, (Class<DataSource>) this.deriveFrom.getClass());
	}

	/**
	 * Create a new {@link DataSourceBuilder} instance.
	 * @return a new datasource builder instance
	 */
	public static DataSourceBuilder<?> create() {
		return create(null);
	}

	/**
	 * Create a new {@link DataSourceBuilder} instance.
	 * @param classLoader the classloader used to discover preferred settings
	 * @return a new {@link DataSource} builder instance
	 */
	public static DataSourceBuilder<?> create(ClassLoader classLoader) {
		return new DataSourceBuilder<>(classLoader);
	}

	/**
	 * Create a new {@link DataSourceBuilder} instance derived from the specified data
	 * source. The returned builder can be used to build the same type of
	 * {@link DataSource} with {@code username}, {@code password}, {@code url} and
	 * {@code driverClassName} properties copied from the original when not specifically
	 * set.
	 * @param dataSource the source {@link DataSource}
	 * @return a new {@link DataSource} builder
	 * @since 2.5.0
	 */
	public static DataSourceBuilder<?> derivedFrom(DataSource dataSource) {
		if (dataSource instanceof EmbeddedDatabase) {
			try {
				dataSource = dataSource.unwrap(DataSource.class);
			}
			catch (SQLException ex) {
				throw new IllegalStateException("Unable to unwrap embedded database", ex);
			}
		}
		return new DataSourceBuilder<>(unwrap(dataSource));
	}

	/**
	 * Unwraps a DataSource object to its original form by recursively calling the
	 * unwrap() method until the original DataSource is obtained.
	 * @param dataSource the DataSource object to be unwrapped
	 * @return the unwrapped DataSource object
	 */
	private static DataSource unwrap(DataSource dataSource) {
		try {
			while (dataSource.isWrapperFor(DataSource.class)) {
				DataSource unwrapped = dataSource.unwrap(DataSource.class);
				if (unwrapped == dataSource) {
					return unwrapped;
				}
				dataSource = unwrapped;
			}
		}
		catch (SQLException ex) {
			// Try to continue with the existing, potentially still wrapped, DataSource
		}
		return dataSource;
	}

	/**
	 * Find the {@link DataSource} type preferred for the given classloader.
	 * @param classLoader the classloader used to discover preferred settings
	 * @return the preferred {@link DataSource} type
	 */
	public static Class<? extends DataSource> findType(ClassLoader classLoader) {
		MappedDataSourceProperties<?> mappings = MappedDataSourceProperties.forType(classLoader, null);
		return (mappings != null) ? mappings.getDataSourceInstanceType() : null;
	}

	/**
	 * An individual DataSource property supported by the builder.
	 */
	private enum DataSourceProperty {

		URL(false, "url", "URL"),

		DRIVER_CLASS_NAME(true, "driverClassName"),

		USERNAME(false, "username", "user"),

		PASSWORD(false, "password");

		private final boolean optional;

		private final String[] names;

		/**
		 * Constructs a new DataSourceProperty object with the specified optional flag and
		 * names.
		 * @param optional the optional flag indicating if the property is optional
		 * @param names the names of the property
		 */
		DataSourceProperty(boolean optional, String... names) {
			this.optional = optional;
			this.names = names;
		}

		/**
		 * Returns a boolean value indicating whether the data source is optional.
		 * @return true if the data source is optional, false otherwise
		 */
		boolean isOptional() {
			return this.optional;
		}

		/**
		 * Returns a string representation of the first element in the names array.
		 * @return a string representation of the first element in the names array
		 */
		@Override
		public String toString() {
			return this.names[0];
		}

		/**
		 * Finds the setter method for the given type in the DataSourceBuilder class.
		 * @param type the type of the setter method to be found
		 * @return the setter method if found, null otherwise
		 */
		Method findSetter(Class<?> type) {
			return findMethod("set", type, String.class);
		}

		/**
		 * Finds the getter method for the specified type.
		 * @param type the class type for which to find the getter method
		 * @return the getter method for the specified type, or null if not found
		 */
		Method findGetter(Class<?> type) {
			return findMethod("get", type);
		}

		/**
		 * Finds a method with the given prefix and parameter types in the specified
		 * class.
		 * @param prefix the prefix to be added to the method name
		 * @param type the class in which to search for the method
		 * @param paramTypes the parameter types of the method
		 * @return the found method, or null if no method is found
		 */
		private Method findMethod(String prefix, Class<?> type, Class<?>... paramTypes) {
			for (String name : this.names) {
				String candidate = prefix + StringUtils.capitalize(name);
				Method method = ReflectionUtils.findMethod(type, candidate, paramTypes);
				if (method != null) {
					return method;
				}
			}
			return null;
		}

	}

	private interface DataSourceProperties<T extends DataSource> {

		Class<? extends T> getDataSourceInstanceType();

		boolean canSet(DataSourceProperty property);

		void set(T dataSource, DataSourceProperty property, String value);

		String get(T dataSource, DataSourceProperty property);

		static <T extends DataSource> DataSourceProperties<T> forType(ClassLoader classLoader, Class<T> type) {
			MappedDataSourceProperties<T> mapped = MappedDataSourceProperties.forType(classLoader, type);
			return (mapped != null) ? mapped : new ReflectionDataSourceProperties<>(type);
		}

	}

	/**
	 * MappedDataSourceProperties class.
	 */
	private static class MappedDataSourceProperties<T extends DataSource> implements DataSourceProperties<T> {

		private final Map<DataSourceProperty, MappedDataSourceProperty<T, ?>> mappedProperties = new HashMap<>();

		private final Class<T> dataSourceType;

		/**
		 * Constructs a new instance of MappedDataSourceProperties. This constructor
		 * initializes the dataSourceType field by resolving the generic type parameter of
		 * the MappedDataSourceProperties class.
		 * @param <T> the type of the dataSourceType field
		 */
		@SuppressWarnings("unchecked")
		MappedDataSourceProperties() {
			this.dataSourceType = (Class<T>) ResolvableType.forClass(MappedDataSourceProperties.class, getClass())
				.resolveGeneric();
		}

		/**
		 * Returns the instance type of the data source.
		 * @return the instance type of the data source
		 */
		@Override
		public Class<? extends T> getDataSourceInstanceType() {
			return this.dataSourceType;
		}

		/**
		 * Adds a new property to the data source properties with the specified getter and
		 * setter.
		 * @param property the data source property to add
		 * @param getter the getter function to retrieve the value of the property
		 * @param setter the setter function to set the value of the property
		 * @param <T> the type of the data source property
		 */
		protected void add(DataSourceProperty property, Getter<T, String> getter, Setter<T, String> setter) {
			add(property, String.class, getter, setter);
		}

		/**
		 * Adds a new mapped data source property to the collection.
		 * @param property the data source property to be added
		 * @param type the class type of the property value
		 * @param getter the getter function to retrieve the property value from the
		 * target object
		 * @param setter the setter function to set the property value on the target
		 * object
		 * @param <V> the type parameter representing the property value
		 */
		protected <V> void add(DataSourceProperty property, Class<V> type, Getter<T, V> getter, Setter<T, V> setter) {
			this.mappedProperties.put(property, new MappedDataSourceProperty<>(property, type, getter, setter));
		}

		/**
		 * Checks if the given property can be set.
		 * @param property the DataSourceProperty to check
		 * @return true if the property can be set, false otherwise
		 */
		@Override
		public boolean canSet(DataSourceProperty property) {
			return this.mappedProperties.containsKey(property);
		}

		/**
		 * Sets the value of a specific property for a given data source.
		 * @param dataSource the data source for which the property value is to be set
		 * @param property the property to set the value for
		 * @param value the value to set for the property
		 */
		@Override
		public void set(T dataSource, DataSourceProperty property, String value) {
			MappedDataSourceProperty<T, ?> mappedProperty = getMapping(property);
			if (mappedProperty != null) {
				mappedProperty.set(dataSource, value);
			}
		}

		/**
		 * Retrieves the value of the specified property from the given data source.
		 * @param dataSource the data source from which to retrieve the property value
		 * @param property the property to retrieve
		 * @return the value of the property, or null if the property is not found
		 */
		@Override
		public String get(T dataSource, DataSourceProperty property) {
			MappedDataSourceProperty<T, ?> mappedProperty = getMapping(property);
			if (mappedProperty != null) {
				return mappedProperty.get(dataSource);
			}
			return null;
		}

		/**
		 * Retrieves the mapping for the given DataSourceProperty.
		 * @param property the DataSourceProperty to retrieve the mapping for
		 * @return the MappedDataSourceProperty associated with the given property
		 * @throws UnsupportedDataSourcePropertyException if the property is not optional
		 * and no mapping is found
		 */
		private MappedDataSourceProperty<T, ?> getMapping(DataSourceProperty property) {
			MappedDataSourceProperty<T, ?> mappedProperty = this.mappedProperties.get(property);
			UnsupportedDataSourcePropertyException.throwIf(!property.isOptional() && mappedProperty == null,
					() -> "No mapping found for " + property);
			return mappedProperty;
		}

		/**
		 * Returns the MappedDataSourceProperties for the specified type.
		 * @param classLoader the class loader to use for loading the type
		 * @param type the type of the data source
		 * @param <T> the type parameter for the data source
		 * @return the MappedDataSourceProperties for the specified type, or null if not
		 * found
		 */
		static <T extends DataSource> MappedDataSourceProperties<T> forType(ClassLoader classLoader, Class<T> type) {
			MappedDataSourceProperties<T> pooled = lookupPooled(classLoader, type);
			if (type == null || pooled != null) {
				return pooled;
			}
			return lookupBasic(classLoader, type);
		}

		/**
		 * Looks up the properties of a pooled data source based on the provided class
		 * loader and data source type.
		 * @param classLoader the class loader to use for the lookup
		 * @param type the type of the data source
		 * @return the mapped data source properties
		 * @param <T> the type of the data source
		 */
		private static <T extends DataSource> MappedDataSourceProperties<T> lookupPooled(ClassLoader classLoader,
				Class<T> type) {
			MappedDataSourceProperties<T> result = null;
			result = lookup(classLoader, type, result, "com.zaxxer.hikari.HikariDataSource",
					HikariDataSourceProperties::new);
			result = lookup(classLoader, type, result, "org.apache.tomcat.jdbc.pool.DataSource",
					TomcatPoolDataSourceProperties::new);
			result = lookup(classLoader, type, result, "org.apache.commons.dbcp2.BasicDataSource",
					MappedDbcp2DataSource::new);
			result = lookup(classLoader, type, result, "oracle.ucp.jdbc.PoolDataSourceImpl",
					OraclePoolDataSourceProperties::new, "oracle.jdbc.OracleConnection");
			result = lookup(classLoader, type, result, "com.mchange.v2.c3p0.ComboPooledDataSource",
					ComboPooledDataSourceProperties::new);
			return result;
		}

		/**
		 * Looks up and returns the basic properties of a mapped data source.
		 * @param classLoader the class loader to use for loading the data source classes
		 * @param dataSourceType the type of the data source
		 * @return the basic properties of the mapped data source
		 */
		private static <T extends DataSource> MappedDataSourceProperties<T> lookupBasic(ClassLoader classLoader,
				Class<T> dataSourceType) {
			MappedDataSourceProperties<T> result = null;
			result = lookup(classLoader, dataSourceType, result,
					"org.springframework.jdbc.datasource.SimpleDriverDataSource", SimpleDataSourceProperties::new);
			result = lookup(classLoader, dataSourceType, result, "oracle.jdbc.datasource.OracleDataSource",
					OracleDataSourceProperties::new);
			result = lookup(classLoader, dataSourceType, result, "org.h2.jdbcx.JdbcDataSource",
					H2DataSourceProperties::new);
			result = lookup(classLoader, dataSourceType, result, "org.postgresql.ds.PGSimpleDataSource",
					PostgresDataSourceProperties::new);
			return result;
		}

		/**
		 * Looks up the MappedDataSourceProperties for a given data source type.
		 * @param classLoader The class loader to use for loading classes.
		 * @param dataSourceType The data source type to lookup.
		 * @param existing The existing MappedDataSourceProperties, if any.
		 * @param dataSourceClassName The class name of the data source.
		 * @param propertyMappingsSupplier The supplier for the property mappings.
		 * @param requiredClassNames The required class names for the data source.
		 * @return The MappedDataSourceProperties for the given data source type, or null
		 * if not found.
		 */
		@SuppressWarnings("unchecked")
		private static <T extends DataSource> MappedDataSourceProperties<T> lookup(ClassLoader classLoader,
				Class<T> dataSourceType, MappedDataSourceProperties<T> existing, String dataSourceClassName,
				Supplier<MappedDataSourceProperties<?>> propertyMappingsSupplier, String... requiredClassNames) {
			if (existing != null || !allPresent(classLoader, dataSourceClassName, requiredClassNames)) {
				return existing;
			}
			MappedDataSourceProperties<?> propertyMappings = propertyMappingsSupplier.get();
			return (dataSourceType == null
					|| propertyMappings.getDataSourceInstanceType().isAssignableFrom(dataSourceType))
							? (MappedDataSourceProperties<T>) propertyMappings : null;
		}

		/**
		 * Checks if all the required classes are present in the given class loader.
		 * @param classLoader the class loader to check for the presence of classes
		 * @param dataSourceClassName the name of the data source class to check for
		 * @param requiredClassNames an array of required class names to check for
		 * @return true if all the required classes are present, false otherwise
		 */
		private static boolean allPresent(ClassLoader classLoader, String dataSourceClassName,
				String[] requiredClassNames) {
			boolean result = ClassUtils.isPresent(dataSourceClassName, classLoader);
			for (String requiredClassName : requiredClassNames) {
				result = result && ClassUtils.isPresent(requiredClassName, classLoader);
			}
			return result;
		}

	}

	/**
	 * MappedDataSourceProperty class.
	 */
	private static class MappedDataSourceProperty<T extends DataSource, V> {

		private final DataSourceProperty property;

		private final Class<V> type;

		private final Getter<T, V> getter;

		private final Setter<T, V> setter;

		/**
		 * Constructs a new MappedDataSourceProperty with the specified
		 * DataSourceProperty, type, getter, and setter.
		 * @param property the DataSourceProperty associated with this
		 * MappedDataSourceProperty
		 * @param type the Class representing the type of the property value
		 * @param getter the Getter function used to retrieve the property value from an
		 * object of type T
		 * @param setter the Setter function used to set the property value on an object
		 * of type T
		 */
		MappedDataSourceProperty(DataSourceProperty property, Class<V> type, Getter<T, V> getter, Setter<T, V> setter) {
			this.property = property;
			this.type = type;
			this.getter = getter;
			this.setter = setter;
		}

		/**
		 * Sets the value of a data source property.
		 * @param dataSource the data source object
		 * @param value the value to set
		 * @throws IllegalStateException if a SQLException occurs during the setting of
		 * the property
		 * @throws UnsupportedDataSourcePropertyException if no setter is mapped for a
		 * non-optional property
		 */
		void set(T dataSource, String value) {
			try {
				if (this.setter == null) {
					UnsupportedDataSourcePropertyException.throwIf(!this.property.isOptional(),
							() -> "No setter mapped for '" + this.property + "' property");
					return;
				}
				this.setter.set(dataSource, convertFromString(value));
			}
			catch (SQLException ex) {
				throw new IllegalStateException(ex);
			}
		}

		/**
		 * Retrieves the value of the specified data source property as a string.
		 * @param dataSource the data source from which to retrieve the property value
		 * @return the property value as a string, or null if no getter is mapped for the
		 * property
		 * @throws IllegalStateException if a SQLException occurs during the retrieval
		 * process
		 * @throws UnsupportedDataSourcePropertyException if no getter is mapped for a
		 * non-optional property
		 */
		String get(T dataSource) {
			try {
				if (this.getter == null) {
					UnsupportedDataSourcePropertyException.throwIf(!this.property.isOptional(),
							() -> "No getter mapped for '" + this.property + "' property");
					return null;
				}
				return convertToString(this.getter.get(dataSource));
			}
			catch (SQLException ex) {
				throw new IllegalStateException(ex);
			}
		}

		/**
		 * Converts a string value to the specified type.
		 * @param value the string value to be converted
		 * @return the converted value of type V
		 * @throws IllegalStateException if the value type is not supported
		 */
		@SuppressWarnings("unchecked")
		private V convertFromString(String value) {
			if (String.class.equals(this.type)) {
				return (V) value;
			}
			if (Class.class.equals(this.type)) {
				return (V) ClassUtils.resolveClassName(value, null);
			}
			throw new IllegalStateException("Unsupported value type " + this.type);
		}

		/**
		 * Converts the given value to a string representation.
		 * @param value the value to be converted
		 * @return the string representation of the value
		 * @throws IllegalStateException if the value type is not supported
		 */
		private String convertToString(V value) {
			if (String.class.equals(this.type)) {
				return (String) value;
			}
			if (Class.class.equals(this.type)) {
				return ((Class<?>) value).getName();
			}
			throw new IllegalStateException("Unsupported value type " + this.type);
		}

	}

	/**
	 * ReflectionDataSourceProperties class.
	 */
	private static class ReflectionDataSourceProperties<T extends DataSource> implements DataSourceProperties<T> {

		private final Map<DataSourceProperty, Method> getters;

		private final Map<DataSourceProperty, Method> setters;

		private final Class<T> dataSourceType;

		/**
		 * Constructs a new ReflectionDataSourceProperties object for the specified
		 * dataSourceType.
		 * @param dataSourceType the class representing the type of the DataSource
		 * @throws IllegalArgumentException if dataSourceType is null
		 */
		ReflectionDataSourceProperties(Class<T> dataSourceType) {
			Assert.state(dataSourceType != null, "No supported DataSource type found");
			Map<DataSourceProperty, Method> getters = new HashMap<>();
			Map<DataSourceProperty, Method> setters = new HashMap<>();
			for (DataSourceProperty property : DataSourceProperty.values()) {
				putIfNotNull(getters, property, property.findGetter(dataSourceType));
				putIfNotNull(setters, property, property.findSetter(dataSourceType));
			}
			this.dataSourceType = dataSourceType;
			this.getters = Collections.unmodifiableMap(getters);
			this.setters = Collections.unmodifiableMap(setters);
		}

		/**
		 * Puts the given property and method into the map if the method is not null.
		 * @param map the map to put the property and method into
		 * @param property the data source property
		 * @param method the method associated with the property
		 */
		private void putIfNotNull(Map<DataSourceProperty, Method> map, DataSourceProperty property, Method method) {
			if (method != null) {
				map.put(property, method);
			}
		}

		/**
		 * Returns the instance type of the data source.
		 * @return the instance type of the data source
		 */
		@Override
		public Class<T> getDataSourceInstanceType() {
			return this.dataSourceType;
		}

		/**
		 * Checks if the given DataSourceProperty can be set.
		 * @param property the DataSourceProperty to check
		 * @return true if the DataSourceProperty can be set, false otherwise
		 */
		@Override
		public boolean canSet(DataSourceProperty property) {
			return this.setters.containsKey(property);
		}

		/**
		 * Sets the value of a specific property in the given data source object.
		 * @param dataSource the data source object to set the property value on
		 * @param property the property to set the value for
		 * @param value the value to set for the property
		 */
		@Override
		public void set(T dataSource, DataSourceProperty property, String value) {
			Method method = getMethod(property, this.setters);
			if (method != null) {
				ReflectionUtils.invokeMethod(method, dataSource, value);
			}
		}

		/**
		 * Retrieves the value of a specific property from a given data source.
		 * @param dataSource the data source from which to retrieve the property value
		 * @param property the property to retrieve
		 * @return the value of the specified property as a string, or null if the
		 * property is not found
		 */
		@Override
		public String get(T dataSource, DataSourceProperty property) {
			Method method = getMethod(property, this.getters);
			if (method != null) {
				return (String) ReflectionUtils.invokeMethod(method, dataSource);
			}
			return null;
		}

		/**
		 * Retrieves the method associated with the given DataSourceProperty from the
		 * provided map of methods.
		 * @param property the DataSourceProperty for which the method needs to be
		 * retrieved
		 * @param methods the map of DataSourceProperty and corresponding methods
		 * @return the method associated with the given DataSourceProperty, or null if not
		 * found
		 * @throws UnsupportedDataSourcePropertyException if the property is not optional
		 * and no suitable method is found
		 */
		private Method getMethod(DataSourceProperty property, Map<DataSourceProperty, Method> methods) {
			Method method = methods.get(property);
			if (method == null) {
				UnsupportedDataSourcePropertyException.throwIf(!property.isOptional(),
						() -> "Unable to find suitable method for " + property);
				return null;
			}
			ReflectionUtils.makeAccessible(method);
			return method;
		}

	}

	@FunctionalInterface
	private interface Getter<T, V> {

		V get(T instance) throws SQLException;

	}

	@FunctionalInterface
	private interface Setter<T, V> {

		void set(T instance, V value) throws SQLException;

	}

	/**
	 * {@link DataSourceProperties} for Hikari.
	 */
	private static class HikariDataSourceProperties extends MappedDataSourceProperties<HikariDataSource> {

		/**
		 * Initializes the HikariDataSourceProperties with default properties.
		 *
		 * This method adds the following properties to the HikariDataSourceProperties: -
		 * URL: The JDBC URL of the data source. This property is used to get and set the
		 * JDBC URL of the HikariDataSource. - DRIVER_CLASS_NAME: The fully qualified
		 * class name of the JDBC driver. This property is used to get and set the driver
		 * class name of the HikariDataSource. - USERNAME: The username used to
		 * authenticate with the data source. This property is used to get and set the
		 * username of the HikariDataSource. - PASSWORD: The password used to authenticate
		 * with the data source. This property is used to get and set the password of the
		 * HikariDataSource.
		 */
		HikariDataSourceProperties() {
			add(DataSourceProperty.URL, HikariDataSource::getJdbcUrl, HikariDataSource::setJdbcUrl);
			add(DataSourceProperty.DRIVER_CLASS_NAME, HikariDataSource::getDriverClassName,
					HikariDataSource::setDriverClassName);
			add(DataSourceProperty.USERNAME, HikariDataSource::getUsername, HikariDataSource::setUsername);
			add(DataSourceProperty.PASSWORD, HikariDataSource::getPassword, HikariDataSource::setPassword);
		}

	}

	/**
	 * {@link DataSourceProperties} for Tomcat Pool.
	 */
	private static class TomcatPoolDataSourceProperties
			extends MappedDataSourceProperties<org.apache.tomcat.jdbc.pool.DataSource> {

		/**
		 * Initializes the properties for the TomcatPoolDataSource.
		 *
		 * This method adds the necessary properties for configuring the Tomcat JDBC
		 * connection pool. The properties include the URL, driver class name, username,
		 * and password.
		 * @param urlGetter The method reference to get the URL property from the Tomcat
		 * JDBC DataSource.
		 * @param urlSetter The method reference to set the URL property for the Tomcat
		 * JDBC DataSource.
		 * @param driverClassNameGetter The method reference to get the driver class name
		 * property from the Tomcat JDBC DataSource.
		 * @param driverClassNameSetter The method reference to set the driver class name
		 * property for the Tomcat JDBC DataSource.
		 * @param usernameGetter The method reference to get the username property from
		 * the Tomcat JDBC DataSource.
		 * @param usernameSetter The method reference to set the username property for the
		 * Tomcat JDBC DataSource.
		 * @param passwordGetter The method reference to get the password property from
		 * the Tomcat JDBC DataSource.
		 * @param passwordSetter The method reference to set the password property for the
		 * Tomcat JDBC DataSource.
		 */
		TomcatPoolDataSourceProperties() {
			add(DataSourceProperty.URL, org.apache.tomcat.jdbc.pool.DataSource::getUrl,
					org.apache.tomcat.jdbc.pool.DataSource::setUrl);
			add(DataSourceProperty.DRIVER_CLASS_NAME, org.apache.tomcat.jdbc.pool.DataSource::getDriverClassName,
					org.apache.tomcat.jdbc.pool.DataSource::setDriverClassName);
			add(DataSourceProperty.USERNAME, org.apache.tomcat.jdbc.pool.DataSource::getUsername,
					org.apache.tomcat.jdbc.pool.DataSource::setUsername);
			add(DataSourceProperty.PASSWORD, org.apache.tomcat.jdbc.pool.DataSource::getPassword,
					org.apache.tomcat.jdbc.pool.DataSource::setPassword);
		}

	}

	/**
	 * {@link DataSourceProperties} for DBCP2.
	 */
	private static class MappedDbcp2DataSource extends MappedDataSourceProperties<BasicDataSource> {

		/**
		 * Configures the properties of the MappedDbcp2DataSource.
		 *
		 * This method sets the URL, driver class name, username, and password properties
		 * of the MappedDbcp2DataSource.
		 * @param url the URL of the database
		 * @param driverClassName the fully qualified name of the JDBC driver class
		 * @param username the username for the database connection
		 * @param password the password for the database connection
		 */
		MappedDbcp2DataSource() {
			add(DataSourceProperty.URL, BasicDataSource::getUrl, BasicDataSource::setUrl);
			add(DataSourceProperty.DRIVER_CLASS_NAME, BasicDataSource::getDriverClassName,
					BasicDataSource::setDriverClassName);
			add(DataSourceProperty.USERNAME, BasicDataSource::getUserName, BasicDataSource::setUsername);
			add(DataSourceProperty.PASSWORD, null, BasicDataSource::setPassword);
		}

	}

	/**
	 * {@link DataSourceProperties} for Oracle Pool.
	 */
	private static class OraclePoolDataSourceProperties extends MappedDataSourceProperties<PoolDataSource> {

		/**
		 * Returns the instance type of the data source used for connection pooling.
		 * @return the instance type of the data source used for connection pooling
		 */
		@Override
		public Class<? extends PoolDataSource> getDataSourceInstanceType() {
			return PoolDataSourceImpl.class;
		}

		/**
		 * Initializes the properties for the OraclePoolDataSource.
		 *
		 * This method adds the necessary properties for configuring the
		 * OraclePoolDataSource. The properties include: - URL: The URL of the database. -
		 * DRIVER_CLASS_NAME: The class name of the connection factory. - USERNAME: The
		 * username for authentication. - PASSWORD: The password for authentication.
		 *
		 * The properties are added using the add() method, which takes the property key,
		 * getter method, and setter method as parameters. The getter and setter methods
		 * are used to retrieve and set the values of the properties.
		 *
		 * @see DataSourceProperty
		 * @see PoolDataSource#getURL()
		 * @see PoolDataSource#setURL(String)
		 * @see PoolDataSource#getConnectionFactoryClassName()
		 * @see PoolDataSource#setConnectionFactoryClassName(String)
		 * @see PoolDataSource#getUser()
		 * @see PoolDataSource#setUser(String)
		 * @see PoolDataSource#setPassword(String)
		 */
		OraclePoolDataSourceProperties() {
			add(DataSourceProperty.URL, PoolDataSource::getURL, PoolDataSource::setURL);
			add(DataSourceProperty.DRIVER_CLASS_NAME, PoolDataSource::getConnectionFactoryClassName,
					PoolDataSource::setConnectionFactoryClassName);
			add(DataSourceProperty.USERNAME, PoolDataSource::getUser, PoolDataSource::setUser);
			add(DataSourceProperty.PASSWORD, null, PoolDataSource::setPassword);
		}

	}

	/**
	 * {@link DataSourceProperties} for C3P0.
	 */
	private static class ComboPooledDataSourceProperties extends MappedDataSourceProperties<ComboPooledDataSource> {

		/**
		 * Initializes the ComboPooledDataSourceProperties object with default properties.
		 *
		 * This method adds the following properties to the
		 * ComboPooledDataSourceProperties object: - URL: The JDBC URL of the data source.
		 * It is retrieved using the getJdbcUrl() method of the ComboPooledDataSource
		 * class and set using the setJdbcUrl() method. - DRIVER_CLASS_NAME: The fully
		 * qualified class name of the JDBC driver. It is retrieved using the
		 * getDriverClass() method of the ComboPooledDataSource class and set using the
		 * setDriverClass() method of the ComboPooledDataSourceProperties class. -
		 * USERNAME: The username for authentication with the data source. It is retrieved
		 * using the getUser() method of the ComboPooledDataSource class and set using the
		 * setUser() method of the ComboPooledDataSource class. - PASSWORD: The password
		 * for authentication with the data source. It is retrieved using the
		 * getPassword() method of the ComboPooledDataSource class and set using the
		 * setPassword() method of the ComboPooledDataSource class.
		 */
		ComboPooledDataSourceProperties() {
			add(DataSourceProperty.URL, ComboPooledDataSource::getJdbcUrl, ComboPooledDataSource::setJdbcUrl);
			add(DataSourceProperty.DRIVER_CLASS_NAME, ComboPooledDataSource::getDriverClass, this::setDriverClass);
			add(DataSourceProperty.USERNAME, ComboPooledDataSource::getUser, ComboPooledDataSource::setUser);
			add(DataSourceProperty.PASSWORD, ComboPooledDataSource::getPassword, ComboPooledDataSource::setPassword);
		}

		/**
		 * Sets the driver class for the given ComboPooledDataSource.
		 * @param dataSource the ComboPooledDataSource to set the driver class for
		 * @param driverClass the driver class to set
		 * @throws IllegalArgumentException if an error occurs while setting the driver
		 * class
		 */
		private void setDriverClass(ComboPooledDataSource dataSource, String driverClass) {
			try {
				dataSource.setDriverClass(driverClass);
			}
			catch (PropertyVetoException ex) {
				throw new IllegalArgumentException(ex);
			}
		}

	}

	/**
	 * {@link DataSourceProperties} for Spring's {@link SimpleDriverDataSource}.
	 */
	private static class SimpleDataSourceProperties extends MappedDataSourceProperties<SimpleDriverDataSource> {

		/**
		 * Constructs a new instance of SimpleDataSourceProperties. This constructor
		 * initializes the properties for a simple data source. The properties include
		 * URL, driver class name, username, and password.
		 *
		 * The URL property is set using the getUrl method of SimpleDriverDataSource. The
		 * driver class name property is set using the getClass method of the driver
		 * object in SimpleDriverDataSource. The username property is set using the
		 * getUsername method of SimpleDriverDataSource. The password property is set
		 * using the getPassword method of SimpleDriverDataSource.
		 *
		 * @since 1.0
		 */
		@SuppressWarnings("unchecked")
		SimpleDataSourceProperties() {
			add(DataSourceProperty.URL, SimpleDriverDataSource::getUrl, SimpleDriverDataSource::setUrl);
			add(DataSourceProperty.DRIVER_CLASS_NAME, Class.class, (dataSource) -> dataSource.getDriver().getClass(),
					SimpleDriverDataSource::setDriverClass);
			add(DataSourceProperty.USERNAME, SimpleDriverDataSource::getUsername, SimpleDriverDataSource::setUsername);
			add(DataSourceProperty.PASSWORD, SimpleDriverDataSource::getPassword, SimpleDriverDataSource::setPassword);
		}

	}

	/**
	 * {@link DataSourceProperties} for Oracle.
	 */
	private static class OracleDataSourceProperties extends MappedDataSourceProperties<OracleDataSource> {

		/**
		 * Initializes the properties for an Oracle data source.
		 *
		 * This method adds the necessary properties for configuring an Oracle data
		 * source, including the URL, username, and password.
		 * @param urlGetter A function that retrieves the current URL value from the
		 * OracleDataSource object.
		 * @param urlSetter A function that sets the URL value for the OracleDataSource
		 * object.
		 * @param userGetter A function that retrieves the current username value from the
		 * OracleDataSource object.
		 * @param userSetter A function that sets the username value for the
		 * OracleDataSource object.
		 * @param passwordSetter A function that sets the password value for the
		 * OracleDataSource object.
		 */
		OracleDataSourceProperties() {
			add(DataSourceProperty.URL, OracleDataSource::getURL, OracleDataSource::setURL);
			add(DataSourceProperty.USERNAME, OracleDataSource::getUser, OracleDataSource::setUser);
			add(DataSourceProperty.PASSWORD, null, OracleDataSource::setPassword);
		}

	}

	/**
	 * {@link DataSourceProperties} for H2.
	 */
	private static class H2DataSourceProperties extends MappedDataSourceProperties<JdbcDataSource> {

		/**
		 * Adds the specified properties for configuring the H2 data source.
		 * @param property The property to add.
		 * @param getter The getter method reference for retrieving the property value.
		 * @param setter The setter method reference for setting the property value.
		 */
		H2DataSourceProperties() {
			add(DataSourceProperty.URL, JdbcDataSource::getUrl, JdbcDataSource::setUrl);
			add(DataSourceProperty.USERNAME, JdbcDataSource::getUser, JdbcDataSource::setUser);
			add(DataSourceProperty.PASSWORD, JdbcDataSource::getPassword, JdbcDataSource::setPassword);
		}

	}

	/**
	 * {@link DataSourceProperties} for Postgres.
	 */
	private static class PostgresDataSourceProperties extends MappedDataSourceProperties<PGSimpleDataSource> {

		/**
		 * Configures the properties for a Postgres data source.
		 *
		 * This method adds the necessary properties for configuring a Postgres data
		 * source, including the URL, username, and password.
		 * @param urlGetter a function that retrieves the current URL value from the data
		 * source
		 * @param urlSetter a function that sets the URL value for the data source
		 * @param usernameGetter a function that retrieves the current username value from
		 * the data source
		 * @param usernameSetter a function that sets the username value for the data
		 * source
		 * @param passwordGetter a function that retrieves the current password value from
		 * the data source
		 * @param passwordSetter a function that sets the password value for the data
		 * source
		 */
		PostgresDataSourceProperties() {
			add(DataSourceProperty.URL, PGSimpleDataSource::getUrl, PGSimpleDataSource::setUrl);
			add(DataSourceProperty.USERNAME, PGSimpleDataSource::getUser, PGSimpleDataSource::setUser);
			add(DataSourceProperty.PASSWORD, PGSimpleDataSource::getPassword, PGSimpleDataSource::setPassword);
		}

	}

}
