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

package org.springframework.boot.autoconfigure.orm.jpa;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitManager;
import org.springframework.util.ClassUtils;

/**
 * Convenient builder for JPA EntityManagerFactory instances. Collects common
 * configuration when constructed and then allows you to create one or more
 * {@link LocalContainerEntityManagerFactoryBean} through a fluent builder pattern. The
 * most common options are covered in the builder, but you can always manipulate the
 * product of the builder if you need more control, before returning it from a
 * <code>@Bean</code> definition.
 * 
 * @author Dave Syer
 * @since 1.1.0
 */
public class EntityManagerFactoryBuilder {

	private JpaVendorAdapter jpaVendorAdapter;

	private PersistenceUnitManager persistenceUnitManager;

	private JpaProperties properties;

	/**
	 * Create a new instance passing in the common pieces that will be shared if multiple
	 * EntityManagerFactory instances are created.
	 * @param jpaVendorAdapter a vendor adapter
	 * @param properties common configuration options, including generic map for JPA
	 * vendor properties
	 * @param persistenceUnitManager optional source of persistence unit information (can
	 * be null)
	 */
	public EntityManagerFactoryBuilder(JpaVendorAdapter jpaVendorAdapter,
			JpaProperties properties, PersistenceUnitManager persistenceUnitManager) {
		this.jpaVendorAdapter = jpaVendorAdapter;
		this.persistenceUnitManager = persistenceUnitManager;
		this.properties = properties;
	}

	public Builder dataSource(DataSource dataSource) {
		return new Builder(dataSource);
	}

	/**
	 * A fluent builder for a LocalContainerEntityManagerFactoryBean.
	 */
	public class Builder {

		private DataSource dataSource;

		private String[] packagesToScan;

		private String persistenceUnit;

		private Builder(DataSource dataSource) {
			this.dataSource = dataSource;
		}

		/**
		 * The names of packages to scan for <code>@Entity</code> annotations.
		 * @param packagesToScan packages to scan
		 * @return the builder for fluent usage
		 */
		public Builder packages(String... packagesToScan) {
			this.packagesToScan = packagesToScan;
			return this;
		}

		/**
		 * The classes whose packages should be scanned for <code>@Entity</code>
		 * annotations.
		 * @param basePackageClasses the classes to use
		 * @return the builder for fluent usage
		 */
		public Builder packages(Class<?>... basePackageClasses) {
			Set<String> packages = new HashSet<String>();
			for (Class<?> type : basePackageClasses) {
				packages.add(ClassUtils.getPackageName(type));
			}
			this.packagesToScan = packages.toArray(new String[0]);
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
		 * properties override any values provided in the {@link JpaProperties} used to
		 * create the builder.
		 * @param properties the properties to use
		 * @return the builder for fluent usage
		 */
		public Builder properties(Map<String, Object> properties) {
			EntityManagerFactoryBuilder.this.properties.getProperties()
					.putAll(properties);
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
			entityManagerFactoryBean
					.setJpaVendorAdapter(EntityManagerFactoryBuilder.this.jpaVendorAdapter);
			entityManagerFactoryBean.setDataSource(this.dataSource);
			entityManagerFactoryBean.setPackagesToScan(this.packagesToScan);
			entityManagerFactoryBean.getJpaPropertyMap().putAll(
					EntityManagerFactoryBuilder.this.properties.getProperties());
			entityManagerFactoryBean.getJpaPropertyMap().putAll(
					EntityManagerFactoryBuilder.this.properties
							.getHibernateProperties(this.dataSource));
			return entityManagerFactoryBean;
		}

	}

}
