/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.autoconfigure;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.util.Assert;

/**
 * Configuration for embedded data sources.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 4.0.0
 * @see DataSourceAutoConfiguration
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DataSourceProperties.class)
public class EmbeddedDataSourceConfiguration implements BeanClassLoaderAware {

	@SuppressWarnings("NullAway.Init")
	private ClassLoader classLoader;

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Bean(destroyMethod = "shutdown")
	EmbeddedDatabase dataSource(DataSourceProperties properties) {
		EmbeddedDatabaseType type = EmbeddedDatabaseConnection.get(this.classLoader).getType();
		String databaseName = properties.determineDatabaseName();
		Assert.state(type != null, "'type' must not be null");
		Assert.state(databaseName != null, "'databaseName' must not be null");
		return new EmbeddedDatabaseBuilder().setType(type).setName(databaseName).build();
	}

}
