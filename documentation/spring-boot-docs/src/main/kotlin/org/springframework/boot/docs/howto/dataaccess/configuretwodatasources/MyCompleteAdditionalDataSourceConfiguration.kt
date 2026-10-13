/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.dataaccess.configuretwodatasources

import com.zaxxer.hikari.HikariDataSource

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class MyCompleteAdditionalDataSourceConfiguration {

	@Qualifier("second")
	@Bean(defaultCandidate = false)
	@ConfigurationProperties("app.datasource")
	fun secondDataSourceProperties(): DataSourceProperties {
		return DataSourceProperties()
	}

	@Qualifier("second")
	@Bean(defaultCandidate = false)
	@ConfigurationProperties("app.datasource.configuration")
	fun secondDataSource(@Qualifier("second") dataSourceProperties: DataSourceProperties): HikariDataSource {
		return dataSourceProperties.initializeDataSourceBuilder().type(HikariDataSource::class.java).build()
	}

}

