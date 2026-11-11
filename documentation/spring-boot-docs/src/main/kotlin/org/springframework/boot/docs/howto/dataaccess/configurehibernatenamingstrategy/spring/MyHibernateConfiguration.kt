/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.dataaccess.configurehibernatenamingstrategy.spring

import org.hibernate.boot.model.naming.Identifier
import org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class MyHibernateConfiguration {

	@Bean
	fun caseSensitivePhysicalNamingStrategy(): PhysicalNamingStrategySnakeCaseImpl {
		return object : PhysicalNamingStrategySnakeCaseImpl() {
			override fun toPhysicalColumnName(logicalName: Identifier, jdbcEnvironment: JdbcEnvironment): Identifier {
				return logicalName
			}
		}
	}

}

