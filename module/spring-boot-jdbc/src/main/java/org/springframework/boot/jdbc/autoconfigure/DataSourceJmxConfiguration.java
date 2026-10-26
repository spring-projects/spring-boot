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
import org.springframework.util.ClassUtils;

/**
 * Configures DataSource related MBeans.
 *
 * @author Stephane Nicoll
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBooleanProperty("spring.jmx.enabled")
class DataSourceJmxConfiguration {

	private static final Log logger = LogFactory.getLog(DataSourceJmxConfiguration.class);

	private static final boolean HIKARI_PRESENT = ClassUtils.isPresent("com.zaxxer.hikari.HikariDataSource",
			DataSourceJmxConfiguration.class.getClassLoader());

	/**
	 * Return whether the given {@link DataSource} registers its own MBeans, in which case
	 * it should not be exposed by the {@link MBeanExporter}.
	 * @param dataSource the data source to check
	 * @return {@code true} if the data source registers its own MBeans
	 */
	static boolean registersItsOwnMBeans(DataSource dataSource) {
		return HIKARI_PRESENT && Hikari.registersItsOwnMBeans(dataSource);
	}

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
			if (registersItsOwnMBeans(this.dataSource)) {
				this.mBeanExporter.ifUnique((exporter) -> exporter.addExcludedBean("dataSource"));
			}
		}

		static boolean registersItsOwnMBeans(DataSource dataSource) {
			HikariDataSource hikariDataSource = DataSourceUnwrapper.unwrap(dataSource, HikariConfigMXBean.class,
					HikariDataSource.class);
			return hikariDataSource != null && hikariDataSource.isRegisterMbeans();
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
