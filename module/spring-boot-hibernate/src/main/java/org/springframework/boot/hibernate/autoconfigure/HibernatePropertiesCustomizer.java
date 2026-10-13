/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.hibernate.autoconfigure;

import java.util.Map;

/**
 * Callback interface that can be implemented by beans wishing to customize the Hibernate
 * properties to customize their auto-configuration prior to use by the auto-configured
 * {@code EntityManagerFactory}.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@FunctionalInterface
public interface HibernatePropertiesCustomizer {

	/**
	 * Customize the specified JPA vendor properties.
	 * @param hibernateProperties the JPA vendor properties to customize
	 */
	void customize(Map<String, Object> hibernateProperties);

}
