/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jpa;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.EntityManagerFactory;

import org.springframework.boot.sql.init.dependency.AbstractBeansOfTypeDependsOnDatabaseInitializationDetector;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitializationDetector;
import org.springframework.core.env.Environment;
import org.springframework.orm.jpa.AbstractEntityManagerFactoryBean;

/**
 * {@link DependsOnDatabaseInitializationDetector} for JPA.
 *
 * @author Andy Wilkinson
 */
class JpaDependsOnDatabaseInitializationDetector extends AbstractBeansOfTypeDependsOnDatabaseInitializationDetector {

	private final Environment environment;

	JpaDependsOnDatabaseInitializationDetector(Environment environment) {
		this.environment = environment;
	}

	@Override
	protected Set<Class<?>> getDependsOnDatabaseInitializationBeanTypes() {
		boolean postpone = this.environment.getProperty("spring.jpa.defer-datasource-initialization", boolean.class,
				false);
		return postpone ? Collections.emptySet()
				: new HashSet<>(Arrays.asList(EntityManagerFactory.class, AbstractEntityManagerFactoryBean.class));
	}

}
