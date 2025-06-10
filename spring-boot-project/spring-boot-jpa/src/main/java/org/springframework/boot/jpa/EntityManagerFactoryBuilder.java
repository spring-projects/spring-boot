/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.jpa;

import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import javax.sql.DataSource;

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitManager;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitPostProcessor;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Convenient builder for JPA EntityManagerFactory instances. Collects common
 * configuration when constructed and then allows you to create one or more
 * {@link LocalContainerEntityManagerFactoryBean} through a fluent builder pattern. The
 * most common options are covered in the builder, but you can always manipulate the
 * product of the builder if you need more control, before returning it from a
 * {@code @Bean} definition.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 4.0.0
 */
public class EntityManagerFactoryBuilder {

	private final JpaVendorAdapter jpaVendorAdapter;

	private final PersistenceUnitManager persistenceUnitManager;

	private final Function<DataSource, Map<String, ?>> jpaPropertiesFactory;

	private final URL persistenceUnitRootLocation;

	private AsyncTaskExecutor bootstrapExecutor;

	private PersistenceUnitPostProcessor[] persistenceUnitPostProcessors;

	/**
	 * Create a new instance passing in the common pieces that will be shared if multiple
	 * EntityManagerFactory instances are created.
	 * @param jpaVendorAdapter a vendor adapter
	 * @param jpaPropertiesFactory the JPA properties to be passed to the persistence
	 * provider, based on the {@linkplain #dataSource(DataSource) configured data source}
	 * @param persistenceUnitManager optional source of persistence unit information (can
	 * be null)
	 * @since 3.4.4
	 */
	public EntityManagerFactoryBuilder(JpaVendorAdapter jpaVendorAdapter,
			Function<DataSource, Map<String, ?>> jpaPropertiesFactory, PersistenceUnitManager persistenceUnitManager) {
		this(jpaVendorAdapter, jpaPropertiesFactory, persistenceUnitManager, null);
	}

	/**
	 * Create a new instance passing in the common pieces that will be shared if multiple
	 * EntityManagerFactory instances are created.
	 * @param jpaVendorAdapter a vendor adapter
	 * @param jpaPropertiesFactory the JPA properties to be passed to the persistence
	 * provider, based on the {@linkplain #dataSource(DataSource) configured data source}
	 * @param persistenceUnitManager optional source of persistence unit information (can
	 * be null)
	 * @param persistenceUnitRootLocation the persistence unit root location to use as a
	 * fallback or {@code null}
	 * @since 3.4.4
	 */
	public EntityManagerFactoryBuilder(JpaVendorAdapter jpaVendorAdapter,
			Function<DataSource, Map<String, ?>> jpaPropertiesFactory, PersistenceUnitManager persistenceUnitManager,
			URL persistenceUnitRootLocation) {
		this.jpaVendorAdapter = jpaVendorAdapter;
		this.persistenceUnitManager = persistenceUnitManager;
		this.jpaPropertiesFactory = jpaPropertiesFactory;
		this.persistenceUnitRootLocation = persistenceUnitRootLocation;
	}

	/**
	 * Create a new {@link Builder} for a {@code EntityManagerFactory} using the settings
	 * of the given instance, and the given {@link DataSource}.
	 * @param dataSource the data source to use
	 * @return a builder to create an {@code EntityManagerFactory}
	 */
	public Builder dataSource(DataSource dataSource) {
		return new Builder(dataSource);
	}

	/**
	 * Configure the bootstrap executor to be used by the
	 * {@link LocalContainerEntityManagerFactoryBean}.
	 * @param bootstrapExecutor the executor
	 * @since 2.1.0
	 */
	public void setBootstrapExecutor(AsyncTaskExecutor bootstrapExecutor) {
		this.bootstrapExecutor = bootstrapExecutor;
	}

	/**
	 * Set the {@linkplain PersistenceUnitPostProcessor persistence unit post processors}
	 * to be applied to the PersistenceUnitInfo used for creating the
	 * {@link LocalContainerEntityManagerFactoryBean}.
	 * @param persistenceUnitPostProcessors the persistence unit post processors to use
	 * @since 2.5.0
	 */
	public void setPersistenceUnitPostProcessors(PersistenceUnitPostProcessor... persistenceUnitPostProcessors) {
		this.persistenceUnitPostProcessors = persistenceUnitPostProcessors;
	}

	/**
	 * A fluent builder for a LocalContainerEntityManagerFactoryBean.
	 */
	public final class Builder {

		private final DataSource dataSource;

		private PersistenceManagedTypes managedTypes;

		private String[] packagesToScan;

		private String persistenceUnit;

		private final Map<String, Object> properties = new HashMap<>();

		private String[] mappingResources;

		private boolean jta;

		private Builder(DataSource dataSource) {
			this.dataSource = dataSource;
		}

		/**
		 * The persistence managed types, providing both the managed entities and packages
		 * the entity manager should consider.
		 * @param managedTypes managed types.
		 * @return the builder for fluent usage
		 */
		public Builder managedTypes(PersistenceManagedTypes managedTypes) {
			this.managedTypes = managedTypes;
			return this;
		}

		/**
		 * The names of packages to scan for {@code @Entity} annotations.
		 * @param packagesToScan packages to scan
		 * @return the builder for fluent usage
		 * @see #managedTypes(PersistenceManagedTypes)
		 */
		public Builder packages(String... packagesToScan) {
			this.packagesToScan = packagesToScan;
			return this;
		}

		/**
		 * The classes whose packages should be scanned for {@code @Entity} annotations.
		 * @param basePackageClasses the classes to use
		 * @return the builder for fluent usage
		 * @see #managedTypes(PersistenceManagedTypes)
		 */
		public Builder packages(Class<?>... basePackageClasses) {
			Set<String> packages = new HashSet<>();
			for (Class<?> type : basePackageClasses) {
				packages.add(ClassUtils.getPackageName(type));
			}
			this.packagesToScan = StringUtils.toStringArray(packages);
			return this;
		}

		/**
		 * The name of the persistence unit. If only building one EntityManagerFactory you
		 * can omit this, but if there are more than one in the same application you
		 * should give them distinct names.
		 * @param persistenceUnit the name of the persistence unit
		 * @return the builder for fluent usage
		 */
		public Builder persistenceUnit(String persistenceUnit) {
			this.persistenceUnit = persistenceUnit;
			return this;
		}

		/**
		 * Generic properties for standard JPA or vendor-specific configuration. These
		 * properties override any values provided in the constructor.
		 * @param properties the properties to use
		 * @return the builder for fluent usage
		 */
		public Builder properties(Map<String, ?> properties) {
			this.properties.putAll(properties);
			return this;
		}

		/**
		 * The mapping resources (equivalent to {@code <mapping-file>} entries in
		 * {@code persistence.xml}) for the persistence unit.
		 * <p>
		 * Note that mapping resources must be relative to the classpath root, e.g.
		 * "META-INF/mappings.xml" or "com/mycompany/repository/mappings.xml", so that
		 * they can be loaded through {@code ClassLoader.getResource()}.
		 * @param mappingResources the mapping resources to use
		 * @return the builder for fluent usage
		 */
		public Builder mappingResources(String... mappingResources) {
			this.mappingResources = mappingResources;
			return this;
		}

		/**
		 * Configure if using a JTA {@link DataSource}, i.e. if
		 * {@link LocalContainerEntityManagerFactoryBean#setDataSource(DataSource)
		 * setDataSource} or
		 * {@link LocalContainerEntityManagerFactoryBean#setJtaDataSource(DataSource)
		 * setJtaDataSource} should be called on the
		 * {@link LocalContainerEntityManagerFactoryBean}.
		 * @param jta if the data source is JTA
		 * @return the builder for fluent usage
		 */
		public Builder jta(boolean jta) {
			this.jta = jta;
			return this;
		}

		public LocalContainerEntityManagerFactoryBean build() {
			LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
			if (EntityManagerFactoryBuilder.this.persistenceUnitManager != null) {
				entityManagerFactoryBean
					.setPersistenceUnitManager(EntityManagerFactoryBuilder.this.persistenceUnitManager);
			}
			if (this.persistenceUnit != null) {
				entityManagerFactoryBean.setPersistenceUnitName(this.persistenceUnit);
			}
			entityManagerFactoryBean.setJpaVendorAdapter(EntityManagerFactoryBuilder.this.jpaVendorAdapter);

			if (this.jta) {
				entityManagerFactoryBean.setJtaDataSource(this.dataSource);
			}
			else {
				entityManagerFactoryBean.setDataSource(this.dataSource);
			}
			if (this.managedTypes != null) {
				entityManagerFactoryBean.setManagedTypes(this.managedTypes);
			}
			else {
				entityManagerFactoryBean.setPackagesToScan(this.packagesToScan);
			}
			Map<String, ?> jpaProperties = EntityManagerFactoryBuilder.this.jpaPropertiesFactory.apply(this.dataSource);
			entityManagerFactoryBean.getJpaPropertyMap().putAll(new LinkedHashMap<>(jpaProperties));
			entityManagerFactoryBean.getJpaPropertyMap().putAll(this.properties);
			if (!ObjectUtils.isEmpty(this.mappingResources)) {
				entityManagerFactoryBean.setMappingResources(this.mappingResources);
			}
			URL rootLocation = EntityManagerFactoryBuilder.this.persistenceUnitRootLocation;
			if (rootLocation != null) {
				entityManagerFactoryBean.setPersistenceUnitRootLocation(rootLocation.toString());
			}
			if (EntityManagerFactoryBuilder.this.bootstrapExecutor != null) {
				entityManagerFactoryBean.setBootstrapExecutor(EntityManagerFactoryBuilder.this.bootstrapExecutor);
			}
			if (EntityManagerFactoryBuilder.this.persistenceUnitPostProcessors != null) {
				entityManagerFactoryBean
					.setPersistenceUnitPostProcessors(EntityManagerFactoryBuilder.this.persistenceUnitPostProcessors);
			}
			return entityManagerFactoryBean;
		}

	}

}
