/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.dataaccess.usemultipleentitymanagers;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.boot.jpa.autoconfigure.JpaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

@Configuration(proxyBeanMethods = false)
public class MyAdditionalEntityManagerFactoryConfiguration {

	@Qualifier("second")
	@Bean(defaultCandidate = false)
	@ConfigurationProperties("app.jpa")
	public JpaProperties secondJpaProperties() {
		return new JpaProperties();
	}

	@Qualifier("second")
	@Bean(defaultCandidate = false)
	public LocalContainerEntityManagerFactoryBean secondEntityManagerFactory(@Qualifier("second") DataSource dataSource,
			@Qualifier("second") JpaProperties jpaProperties) {
		EntityManagerFactoryBuilder builder = createEntityManagerFactoryBuilder(jpaProperties);
		return builder.dataSource(dataSource).packages(Order.class).persistenceUnit("second").build();
	}

	private EntityManagerFactoryBuilder createEntityManagerFactoryBuilder(JpaProperties jpaProperties) {
		JpaVendorAdapter jpaVendorAdapter = createJpaVendorAdapter(jpaProperties);
		Function<DataSource, Map<String, ?>> jpaPropertiesFactory = (dataSource) -> createJpaProperties(dataSource,
				jpaProperties.getProperties());
		return new EntityManagerFactoryBuilder(jpaVendorAdapter, jpaPropertiesFactory, null);
	}

	private JpaVendorAdapter createJpaVendorAdapter(JpaProperties jpaProperties) {
		// ... map JPA properties as needed
		return new HibernateJpaVendorAdapter();
	}

	private Map<String, ?> createJpaProperties(DataSource dataSource, Map<String, ?> existingProperties) {
		Map<String, ?> jpaProperties = new LinkedHashMap<>(existingProperties);
		// ... map JPA properties that require the DataSource (e.g. DDL flags)
		return jpaProperties;
	}

}
