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

package org.springframework.boot.autoconfigure.jdbc;

import java.sql.SQLException;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tomcat.jdbc.pool.DataSourceProxy;

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
@Configuration
@ConditionalOnProperty(prefix = "spring.jmx", name = "enabled", havingValue = "true", matchIfMissing = true)
class DataSourceJmxConfiguration {

	private static final Log logger = LogFactory.getLog(DataSourceJmxConfiguration.class);

	@Configuration
	@ConditionalOnClass(HikariDataSource.class)
	@ConditionalOnSingleCandidate(DataSource.class)
	static class Hikari {

		private final DataSource dataSource;

		private final ObjectProvider<MBeanExporter> mBeanExporter;

		Hikari(DataSource dataSource, ObjectProvider<MBeanExporter> mBeanExporter) {
			this.dataSource = dataSource;
			this.mBeanExporter = mBeanExporter;
		}

		@PostConstruct
		public void validateMBeans() {
			HikariDataSource hikariDataSource = DataSourceUnwrapper.unwrap(this.dataSource, HikariDataSource.class);
			if (hikariDataSource != null && hikariDataSource.isRegisterMbeans()) {
				this.mBeanExporter.ifUnique((exporter) -> exporter.addExcludedBean("dataSource"));
			}
		}

	}

	@Configuration
	@ConditionalOnProperty(prefix = "spring.datasource", name = "jmx-enabled")
	@ConditionalOnClass(name = "org.apache.tomcat.jdbc.pool.DataSourceProxy")
	@ConditionalOnSingleCandidate(DataSource.class)
	static class TomcatDataSourceJmxConfiguration {

		@Bean
		@ConditionalOnMissingBean(name = "dataSourceMBean")
		public Object dataSourceMBean(DataSource dataSource) {
			DataSourceProxy dataSourceProxy = DataSourceUnwrapper.unwrap(dataSource, DataSourceProxy.class);
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
