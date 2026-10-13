/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.autoconfigure;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnCheckpointRestore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.jdbc.HikariCheckpointRestoreLifecycle;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Checkpoint-restore specific configuration.
 *
 * @author Olga Maciaszek-Sharma
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnCheckpointRestore
@ConditionalOnBean(DataSource.class)
class DataSourceCheckpointRestoreConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(HikariDataSource.class)
	static class Hikari {

		@Bean
		@ConditionalOnMissingBean
		HikariCheckpointRestoreLifecycle hikariCheckpointRestoreLifecycle(DataSource dataSource,
				ConfigurableApplicationContext applicationContext) {
			return new HikariCheckpointRestoreLifecycle(dataSource, applicationContext);
		}

	}

}
