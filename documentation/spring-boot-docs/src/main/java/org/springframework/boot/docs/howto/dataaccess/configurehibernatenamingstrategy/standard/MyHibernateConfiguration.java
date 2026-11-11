/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.dataaccess.configurehibernatenamingstrategy.standard;

import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class MyHibernateConfiguration {

	@Bean
	PhysicalNamingStrategyStandardImpl caseSensitivePhysicalNamingStrategy() {
		return new PhysicalNamingStrategyStandardImpl();
	}

}
