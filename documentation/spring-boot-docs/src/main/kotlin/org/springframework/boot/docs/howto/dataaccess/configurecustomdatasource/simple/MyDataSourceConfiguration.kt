/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.dataaccess.configurecustomdatasource.simple

import com.zaxxer.hikari.HikariDataSource
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class MyDataSourceConfiguration {

	@Bean
	@ConfigurationProperties("app.datasource")
	fun dataSource(): HikariDataSource {
		return DataSourceBuilder.create().type(HikariDataSource::class.java).build()
	}

}

