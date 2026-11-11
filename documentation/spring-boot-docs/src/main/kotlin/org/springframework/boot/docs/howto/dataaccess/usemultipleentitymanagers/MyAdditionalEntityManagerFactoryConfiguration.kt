/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.dataaccess.usemultipleentitymanagers

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.jpa.EntityManagerFactoryBuilder
import org.springframework.boot.jpa.autoconfigure.JpaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.orm.jpa.JpaVendorAdapter
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import javax.sql.DataSource

@Suppress("UNUSED_PARAMETER")
@Configuration(proxyBeanMethods = false)
class MyAdditionalEntityManagerFactoryConfiguration {

	@Qualifier("second")
	@Bean(defaultCandidate = false)
	@ConfigurationProperties("app.jpa")
	fun secondJpaProperties(): JpaProperties {
		return JpaProperties()
	}

	@Qualifier("second")
	@Bean(defaultCandidate = false)
	fun secondEntityManagerFactory(
		@Qualifier("second") dataSource: DataSource,
		@Qualifier("second") jpaProperties: JpaProperties
	): LocalContainerEntityManagerFactoryBean {
		val builder = createEntityManagerFactoryBuilder(jpaProperties)
		return builder.dataSource(dataSource).packages(Order::class.java).persistenceUnit("second").build()
	}

	private fun createEntityManagerFactoryBuilder(jpaProperties: JpaProperties): EntityManagerFactoryBuilder {
		val jpaVendorAdapter = createJpaVendorAdapter(jpaProperties)
		val jpaPropertiesFactory = { dataSource: DataSource ->
				createJpaProperties(dataSource, jpaProperties.properties) }
		return EntityManagerFactoryBuilder(jpaVendorAdapter, jpaPropertiesFactory, null)
	}

	private fun createJpaVendorAdapter(jpaProperties: JpaProperties): JpaVendorAdapter {
		// ... map JPA properties as needed
		return HibernateJpaVendorAdapter()
	}

	private fun createJpaProperties(dataSource: DataSource, existingProperties: Map<String, *>): Map<String, *> {
		val jpaProperties: Map<String, *> = LinkedHashMap(existingProperties)
		// ... map JPA properties that require the DataSource (e.g. DDL flags)
		return jpaProperties
	}

}

