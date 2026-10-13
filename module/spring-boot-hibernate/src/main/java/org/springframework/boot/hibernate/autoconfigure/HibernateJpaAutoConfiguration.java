/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.hibernate.autoconfigure;

import jakarta.persistence.EntityManager;
import org.hibernate.engine.spi.SessionImplementor;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.jpa.autoconfigure.JpaProperties;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.boot.transaction.autoconfigure.TransactionManagerCustomizationAutoConfiguration;
import org.springframework.boot.transaction.jta.autoconfigure.JtaAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Hibernate JPA.
 *
 * @author Phillip Webb
 * @author Josh Long
 * @author Manuel Doninger
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@AutoConfiguration(
		after = { DataSourceAutoConfiguration.class, JtaAutoConfiguration.class,
				TransactionManagerCustomizationAutoConfiguration.class },
		before = { TransactionAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class })
@ConditionalOnClass({ LocalContainerEntityManagerFactoryBean.class, EntityManager.class, SessionImplementor.class })
@EnableConfigurationProperties(JpaProperties.class)
@Import(HibernateJpaConfiguration.class)
public final class HibernateJpaAutoConfiguration {

}
