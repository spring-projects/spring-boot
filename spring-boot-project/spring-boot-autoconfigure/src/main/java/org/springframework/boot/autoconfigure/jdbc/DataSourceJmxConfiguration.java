/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.jdbc;

import java.sql.SQLException;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfigMXBean;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tomcat.jdbc.pool.DataSourceProxy;
import org.apache.tomcat.jdbc.pool.PoolConfiguration;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.jdbc.DataSourceUnwrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jmx.export.MBeanExporter;

/**
 * Configures DataSource related MBeans.
 *
 * @author Stephane Nicoll
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "spring.jmx", name = "enabled", havingValue = "true", matchIfMissing = true)
class DataSourceJmxConfiguration {

	private static final Log logger = LogFactory.getLog(DataSourceJmxConfiguration.class);

	/**
	 * Hikari class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(HikariDataSource.class)
	@ConditionalOnSingleCandidate(DataSource.class)
	static class Hikari {

		private final DataSource dataSource;

		private final ObjectProvider<MBeanExporter> mBeanExporter;

		/**
		 * Constructs a new instance of the Hikari class with the specified data source
		 * and MBean exporter.
		 * @param dataSource the data source to be used by the Hikari instance
		 * @param mBeanExporter the MBean exporter to be used by the Hikari instance
		 */
		Hikari(DataSource dataSource, ObjectProvider<MBeanExporter> mBeanExporter) {
			this.dataSource = dataSource;
			this.mBeanExporter = mBeanExporter;
			validateMBeans();
		}

		/**
		 * Validates the MBeans for the HikariDataSource.
		 *
		 * This method checks if the HikariDataSource is registered as an MBean and if it
		 * is, it adds an exclusion for the "dataSource" bean to the MBeanExporter.
		 * @throws IllegalArgumentException if the HikariDataSource cannot be unwrapped or
		 * if it is not registered as an MBean.
		 */
		private void validateMBeans() {
			HikariDataSource hikariDataSource = DataSourceUnwrapper.unwrap(this.dataSource, HikariConfigMXBean.class,
					HikariDataSource.class);
			if (hikariDataSource != null && hikariDataSource.isRegisterMbeans()) {
				this.mBeanExporter.ifUnique((exporter) -> exporter.addExcludedBean("dataSource"));
			}
		}

	}

	/**
	 * TomcatDataSourceJmxConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(prefix = "spring.datasource.tomcat", name = "jmx-enabled")
	@ConditionalOnClass(DataSourceProxy.class)
	@ConditionalOnSingleCandidate(DataSource.class)
	static class TomcatDataSourceJmxConfiguration {

		/**
		 * Creates a DataSource MBean if it is missing.
		 * @param dataSource The DataSource object.
		 * @return The DataSource MBean object.
		 */
		@Bean
		@ConditionalOnMissingBean(name = "dataSourceMBean")
		Object dataSourceMBean(DataSource dataSource) {
			DataSourceProxy dataSourceProxy = DataSourceUnwrapper.unwrap(dataSource, PoolConfiguration.class,
					DataSourceProxy.class);
			if (dataSourceProxy != null) {
				try {
					return dataSourceProxy.createPool().getJmxPool();
				}
				catch (SQLException ex) {
					logger.warn("Cannot expose DataSource to JMX (could not connect)");
				}
			}
			return null;
		}

	}

}
