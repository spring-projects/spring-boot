/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.autoconfigure;

import javax.sql.DataSource;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.sql.autoconfigure.init.ApplicationScriptDatabaseInitializer;
import org.springframework.boot.sql.autoconfigure.init.ConditionalOnSqlInitialization;
import org.springframework.boot.sql.autoconfigure.init.SqlInitializationProperties;
import org.springframework.boot.sql.init.dependency.DatabaseInitializationDependencyConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.util.StringUtils;

/**
 * Auto-configuration for {@link DataSource} initialization.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnMissingBean(ApplicationScriptDatabaseInitializer.class)
@ConditionalOnSingleCandidate(DataSource.class)
@ConditionalOnClass(DatabasePopulator.class)
@Import(DatabaseInitializationDependencyConfigurer.class)
@EnableConfigurationProperties(SqlInitializationProperties.class)
@ConditionalOnSqlInitialization
public final class DataSourceInitializationAutoConfiguration {

	@Bean
	ApplicationDataSourceScriptDatabaseInitializer dataSourceScriptDatabaseInitializer(DataSource dataSource,
			SqlInitializationProperties properties) {
		return new ApplicationDataSourceScriptDatabaseInitializer(
				determineDataSource(dataSource, properties.getUsername(), properties.getPassword()), properties);
	}

	private static DataSource determineDataSource(DataSource dataSource, @Nullable String username,
			@Nullable String password) {
		if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
			return DataSourceBuilder.derivedFrom(dataSource)
				.username(username)
				.password(password)
				.type(SimpleDriverDataSource.class)
				.build();
		}
		return dataSource;
	}

}
