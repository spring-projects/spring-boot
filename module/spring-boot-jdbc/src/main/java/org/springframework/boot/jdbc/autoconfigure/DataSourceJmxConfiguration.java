/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.autoconfigure;

import java.sql.SQLException;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfigMXBean;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tomcat.jdbc.pool.DataSourceProxy;
import org.apache.tomcat.jdbc.pool.PoolConfiguration;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
@ConditionalOnBooleanProperty("spring.jmx.enabled")
class DataSourceJmxConfiguration {

	private static final Log logger = LogFactory.getLog(DataSourceJmxConfiguration.class);

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(HikariDataSource.class)
	@ConditionalOnSingleCandidate(DataSource.class)
	static class Hikari {

		private final DataSource dataSource;

		private final ObjectProvider<MBeanExporter> mBeanExporter;

		Hikari(DataSource dataSource, ObjectProvider<MBeanExporter> mBeanExporter) {
			this.dataSource = dataSource;
			this.mBeanExporter = mBeanExporter;
			validateMBeans();
		}

		private void validateMBeans() {
			HikariDataSource hikariDataSource = DataSourceUnwrapper.unwrap(this.dataSource, HikariConfigMXBean.class,
					HikariDataSource.class);
			if (hikariDataSource != null && hikariDataSource.isRegisterMbeans()) {
				this.mBeanExporter.ifUnique((exporter) -> exporter.addExcludedBean("dataSource"));
			}
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBooleanProperty("spring.datasource.tomcat.jmx-enabled")
	@ConditionalOnClass(DataSourceProxy.class)
	@ConditionalOnSingleCandidate(DataSource.class)
	static class TomcatDataSourceJmxConfiguration {

		@Bean
		@ConditionalOnMissingBean(name = "dataSourceMBean")
		@Nullable Object dataSourceMBean(DataSource dataSource) {
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
