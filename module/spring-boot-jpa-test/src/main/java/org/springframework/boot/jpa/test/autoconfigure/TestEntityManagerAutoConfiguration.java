/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jpa.test.autoconfigure;

import jakarta.persistence.EntityManagerFactory;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for {@link TestEntityManager}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 * @see AutoConfigureTestEntityManager
 */
@AutoConfiguration(afterName = "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration")
@ConditionalOnClass({ EntityManagerFactory.class })
public final class TestEntityManagerAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	TestEntityManager testEntityManager(EntityManagerFactory entityManagerFactory) {
		return new TestEntityManager(entityManagerFactory);
	}

}
