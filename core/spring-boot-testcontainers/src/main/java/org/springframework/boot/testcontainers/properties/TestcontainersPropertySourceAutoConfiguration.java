/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testcontainers.properties;

import org.testcontainers.containers.GenericContainer;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.support.DynamicPropertyRegistrarBeanInitializer;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} to add support for properties sourced from a Testcontainers
 * {@link GenericContainer container}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 3.1.0
 */
@AutoConfiguration
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnClass(DynamicPropertyRegistry.class)
public final class TestcontainersPropertySourceAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	static DynamicPropertyRegistrarBeanInitializer dynamicPropertyRegistrarBeanInitializer() {
		return new DynamicPropertyRegistrarBeanInitializer();
	}

}
