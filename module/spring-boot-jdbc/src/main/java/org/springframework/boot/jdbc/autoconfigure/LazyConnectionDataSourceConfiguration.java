/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.autoconfigure;

import javax.sql.DataSource;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.core.Ordered;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.support.JmxUtils;

/**
 * Replace the auto-configured {@link DataSource} by a
 * {@linkplain LazyConnectionDataSourceProxy lazy proxy} that fetches the underlying JDBC
 * connection as late as possible. Also make sure to register the target
 * {@link DataSource} in the JMX domain if necessary.
 *
 * @author Stephane Nicoll
 */
@Configuration
@ConditionalOnBean(DataSource.class)
@ConditionalOnProperty(name = "spring.datasource.connection-fetch", havingValue = "lazy")
class LazyConnectionDataSourceConfiguration {

	@Bean
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	static LazyConnectionDataSourceBeanPostProcessor lazyConnectionDataSourceBeanPostProcessor(
			ObjectProvider<MBeanExporter> mbeanExporter) {
		return new LazyConnectionDataSourceBeanPostProcessor(mbeanExporter);
	}

	static class LazyConnectionDataSourceBeanPostProcessor implements BeanPostProcessor, Ordered {

		private final ObjectProvider<MBeanExporter> mbeanExporter;

		LazyConnectionDataSourceBeanPostProcessor(ObjectProvider<MBeanExporter> mbeanExporter) {
			this.mbeanExporter = mbeanExporter;
		}

		@Override
		public @Nullable Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
			if (beanName.equals("dataSource") && bean instanceof DataSource dataSource) {
				this.mbeanExporter.ifAvailable((exporter) -> {
					if (JmxUtils.isMBean(dataSource.getClass())) {
						exporter.registerManagedResource(dataSource, beanName);
					}
				});
				return new LazyConnectionDataSourceProxy(dataSource);
			}
			return bean;
		}

		@Override
		public int getOrder() {
			return 0;
		}

	}

}
