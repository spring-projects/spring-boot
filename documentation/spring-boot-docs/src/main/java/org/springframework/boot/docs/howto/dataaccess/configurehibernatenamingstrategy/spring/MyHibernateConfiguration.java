/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.dataaccess.configurehibernatenamingstrategy.spring;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class MyHibernateConfiguration {

	@Bean
	public PhysicalNamingStrategySnakeCaseImpl caseSensitivePhysicalNamingStrategy() {
		return new PhysicalNamingStrategySnakeCaseImpl() {

			@Override
			public Identifier toPhysicalColumnName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
				return logicalName;
			}

		};
	}

}
