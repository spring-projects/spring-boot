/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.dataaccess.configurecustomdatasource.configurable

import com.zaxxer.hikari.HikariDataSource
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration(proxyBeanMethods = false)
class MyDataSourceConfiguration {

	@Bean
	@Primary
	@ConfigurationProperties("app.datasource")
	fun dataSourceProperties(): DataSourceProperties {
		return DataSourceProperties()
	}

	@Bean
	@ConfigurationProperties("app.datasource.configuration")
	fun dataSource(properties: DataSourceProperties): HikariDataSource {
		return properties.initializeDataSourceBuilder().type(HikariDataSource::class.java).build()
	}

}

