/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.liquibase.autoconfigure;

import liquibase.integration.spring.SpringLiquibase;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.liquibase.actuate.endpoint.LiquibaseEndpoint;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link LiquibaseEndpoint}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
@AutoConfiguration(after = LiquibaseAutoConfiguration.class)
@ConditionalOnClass({ SpringLiquibase.class, LiquibaseEndpoint.class, ConditionalOnAvailableEndpoint.class })
@ConditionalOnAvailableEndpoint(LiquibaseEndpoint.class)
public final class LiquibaseEndpointAutoConfiguration {

	@Bean
	@ConditionalOnBean(SpringLiquibase.class)
	@ConditionalOnMissingBean
	LiquibaseEndpoint liquibaseEndpoint(ApplicationContext context) {
		return new LiquibaseEndpoint(context);
	}

	@Bean
	@ConditionalOnBean(SpringLiquibase.class)
	static BeanPostProcessor preventDataSourceCloseBeanPostProcessor() {
		return new BeanPostProcessor() {

			@Override
			public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
				if (bean instanceof DataSourceClosingSpringLiquibase dataSource) {
					dataSource.setCloseDataSourceOnceMigrated(false);
				}
				return bean;
			}

			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
				return bean;
			}

		};
	}

}
