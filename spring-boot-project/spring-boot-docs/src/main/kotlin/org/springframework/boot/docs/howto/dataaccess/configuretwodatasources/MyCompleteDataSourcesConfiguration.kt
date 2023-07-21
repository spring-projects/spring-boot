/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.docs.howto.dataaccess.configuretwodatasources

import com.zaxxer.hikari.HikariDataSource
import org.apache.commons.dbcp2.BasicDataSource
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration(proxyBeanMethods = false)
class MyCompleteDataSourcesConfiguration {

	@Bean
	@Primary
	@ConfigurationProperties("app.datasource.first")
	fun firstDataSourceProperties(): DataSourceProperties {
		return DataSourceProperties()
	}

	@Bean
	@Primary
	@ConfigurationProperties("app.datasource.first.configuration")
	fun firstDataSource(firstDataSourceProperties: DataSourceProperties): HikariDataSource {
		return firstDataSourceProperties.initializeDataSourceBuilder().type(HikariDataSource::class.java).build()
	}

	@Bean
	@ConfigurationProperties("app.datasource.second")
	fun secondDataSourceProperties(): DataSourceProperties {
		return DataSourceProperties()
	}

	@Bean
	@ConfigurationProperties("app.datasource.second.configuration")
	fun secondDataSource(secondDataSourceProperties: DataSourceProperties): BasicDataSource {
		return secondDataSourceProperties.initializeDataSourceBuilder().type(BasicDataSource::class.java).build()
	}

}
