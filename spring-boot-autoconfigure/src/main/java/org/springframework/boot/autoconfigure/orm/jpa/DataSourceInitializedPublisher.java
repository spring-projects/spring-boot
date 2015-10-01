/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.orm.jpa;

import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.autoconfigure.jdbc.DataSourceInitializedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

/**
 * {@link BeanPostProcessor} used to fire {@link DataSourceInitializedEvent}s. Should only
 * be registered via the inner {@link Registrar} class.
 *
 * @author Dave Syer
 * @since 1.1.0
 */
class DataSourceInitializedPublisher implements BeanPostProcessor {

	@Autowired
	private ApplicationContext applicationContext;

	private DataSource dataSource;

	private JpaProperties properties;

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {
		if (bean instanceof DataSource) {
			// Normally this will be the right DataSource
			this.dataSource = (DataSource) bean;
		}
		if (bean instanceof JpaProperties) {
			this.properties = (JpaProperties) bean;
		}
		if (bean instanceof EntityManagerFactory && this.dataSource != null
				&& isInitializingDatabase()) {
			this.applicationContext.publishEvent(new DataSourceInitializedEvent(
					this.dataSource));
		}
		return bean;
	}

	private boolean isInitializingDatabase() {
		if (this.properties == null) {
			return true; // better safe than sorry
		}
		Map<String, String> hibernate = this.properties
				.getHibernateProperties(this.dataSource);
		if (hibernate.containsKey("hibernate.hbm2ddl.auto")) {
			return true;
		}
		return false;
	}

	/**
	 * {@link ImportBeanDefinitionRegistrar} to register the
	 * {@link DataSourceInitializedPublisher} without causing early bean instantiation
	 * issues.
	 */
	static class Registrar implements ImportBeanDefinitionRegistrar {

		private static final String BEAN_NAME = "dataSourceInitializedPublisher";

		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
				BeanDefinitionRegistry registry) {
			if (!registry.containsBeanDefinition(BEAN_NAME)) {
				GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
				beanDefinition.setBeanClass(DataSourceInitializedPublisher.class);
				beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
				// We don't need this one to be post processed otherwise it can cause a
				// cascade of bean instantiation that we would rather avoid.
				beanDefinition.setSynthetic(true);
				registry.registerBeanDefinition(BEAN_NAME, beanDefinition);
			}
		}
	}

}
