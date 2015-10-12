/*
 * Copyright 2012-2015 the original author or authors.
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

import java.util.Map;

import javax.sql.DataSource;

import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitManager;

/**
 * Convenient builder for JPA EntityManagerFactory instances. Collects common
 * configuration when constructed and then allows you to create one or more
 * {@link LocalContainerEntityManagerFactoryBean} through a fluent builder pattern. The
 * most common options are covered in the builder, but you can always manipulate the
 * product of the builder if you need more control, before returning it from a
 * {@code @Bean} definition.
 *
 * @author Dave Syer
 * @since 1.1.0
 * @deprecated since 1.3.0 in favor of
 * {@link org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder}
 */
@Deprecated
public class EntityManagerFactoryBuilder {

	private final Delegate delegate;

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
		this.delegate = new Delegate(jpaVendorAdapter, properties.getProperties(),
				persistenceUnitManager);
	}

	public Builder dataSource(DataSource dataSource) {
		return new Builder(this.delegate.dataSource(dataSource));
	}

	/**
	 * An optional callback for new entity manager factory beans.
	 * @param callback the entity manager factory bean callback
	 */
	public void setCallback(final EntityManagerFactoryBeanCallback callback) {
		this.delegate.setCallback(
				new org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder.EntityManagerFactoryBeanCallback() {

					@Override
					public void execute(LocalContainerEntityManagerFactoryBean factory) {
						callback.execute(factory);
					}

				});
	}

	/**
	 * A fluent builder for a LocalContainerEntityManagerFactoryBean.
	 * @deprecated since 1.3.0 in favor of
	 * {@link org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder}
	 */
	@Deprecated
	public final class Builder {

		private final org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder.Builder delegate;

		private Builder(
				org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder.Builder delegate) {
			this.delegate = delegate;
		}

		/**
		 * The names of packages to scan for {@code @Entity} annotations.
		 * @param packagesToScan packages to scan
		 * @return the builder for fluent usage
		 */
		public Builder packages(String... packagesToScan) {
			this.delegate.packages(packagesToScan);
			return this;
		}

		/**
		 * The classes whose packages should be scanned for {@code @Entity} annotations.
		 * @param basePackageClasses the classes to use
		 * @return the builder for fluent usage
		 */
		public Builder packages(Class<?>... basePackageClasses) {
			this.delegate.packages(basePackageClasses);
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
			this.delegate.persistenceUnit(persistenceUnit);
			return this;
		}

		/**
		 * Generic properties for standard JPA or vendor-specific configuration. These
		 * properties override any values provided in the {@link JpaProperties} used to
		 * create the builder.
		 * @param properties the properties to use
		 * @return the builder for fluent usage
		 */
		public Builder properties(Map<String, ?> properties) {
			this.delegate.properties(properties);
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
			this.delegate.jta(jta);
			return this;
		}

		public LocalContainerEntityManagerFactoryBean build() {
			return this.delegate.build();
		}

	}

	/**
	 * A callback for new entity manager factory beans created by a Builder.
	 * @deprecated since 1.3.0 in favor of
	 * {@link org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder}
	 */
	@Deprecated
	public interface EntityManagerFactoryBeanCallback {

		void execute(LocalContainerEntityManagerFactoryBean factory);

	}

	private static class Delegate
			extends org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder {

		Delegate(JpaVendorAdapter jpaVendorAdapter, Map<String, ?> jpaProperties,
				PersistenceUnitManager persistenceUnitManager) {
			super(jpaVendorAdapter, jpaProperties, persistenceUnitManager);
		}

	}

}
