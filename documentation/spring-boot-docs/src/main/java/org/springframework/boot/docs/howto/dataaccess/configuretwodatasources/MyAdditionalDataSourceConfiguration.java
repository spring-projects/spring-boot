/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.dataaccess.configuretwodatasources;

import com.zaxxer.hikari.HikariDataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class MyAdditionalDataSourceConfiguration {

	@Qualifier("second")
	@Bean(defaultCandidate = false)
	@ConfigurationProperties("app.datasource")
	public HikariDataSource secondDataSource() {
		return DataSourceBuilder.create().type(HikariDataSource.class).build();
	}

}
