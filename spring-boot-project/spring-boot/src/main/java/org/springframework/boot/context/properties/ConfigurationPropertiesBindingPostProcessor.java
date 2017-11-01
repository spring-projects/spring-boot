/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.context.properties;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySources;
import org.springframework.core.env.StandardEnvironment;

/**
 * {@link BeanPostProcessor} to bind {@link PropertySources} to beans annotated with
 * {@link ConfigurationProperties}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Christian Dupuis
 * @author Stephane Nicoll
 * @author Madhura Bhave
 */
public class ConfigurationPropertiesBindingPostProcessor
		implements BeanPostProcessor, BeanFactoryAware, EnvironmentAware,
		ApplicationContextAware, InitializingBean, PriorityOrdered {

	private static final Log logger = LogFactory
			.getLog(ConfigurationPropertiesBindingPostProcessor.class);

	private ConfigurationBeanFactoryMetaData beans = new ConfigurationBeanFactoryMetaData();

	private BeanFactory beanFactory;

	private Environment environment = new StandardEnvironment();

	private ApplicationContext applicationContext;

	private ConfigurationPropertiesBinder configurationPropertiesBinder;

	private PropertySources propertySources;

	/**
	 * Return the order of the bean.
	 * @return the order
	 */
	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE + 1;
	}

	/**
	 * Set the bean meta-data store.
	 * @param beans the bean meta data store
	 */
	public void setBeanMetaDataStore(ConfigurationBeanFactoryMetaData beans) {
		this.beans = beans;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.propertySources = deducePropertySources();
	}

	private PropertySources deducePropertySources() {
		MutablePropertySources environmentPropertySources = extractEnvironmentPropertySources();
		PropertySourcesPlaceholderConfigurer configurer = getSinglePropertySourcesPlaceholderConfigurer();
		if (configurer == null) {
			if (environmentPropertySources != null) {
				return environmentPropertySources;
			}
			throw new IllegalStateException("Unable to obtain PropertySources from "
					+ "PropertySourcesPlaceholderConfigurer or Environment");
		}
		PropertySources appliedPropertySources = configurer.getAppliedPropertySources();
		if (environmentPropertySources == null) {
			return appliedPropertySources;
		}
		return new CompositePropertySources(
				new FilteredPropertySources(appliedPropertySources,
						PropertySourcesPlaceholderConfigurer.ENVIRONMENT_PROPERTIES_PROPERTY_SOURCE_NAME),
				environmentPropertySources);
	}

	private MutablePropertySources extractEnvironmentPropertySources() {
		if (this.environment instanceof ConfigurableEnvironment) {
			return ((ConfigurableEnvironment) this.environment).getPropertySources();
		}
		return null;
	}

	private PropertySourcesPlaceholderConfigurer getSinglePropertySourcesPlaceholderConfigurer() {
		// Take care not to cause early instantiation of all FactoryBeans
		if (this.beanFactory instanceof ListableBeanFactory) {
			ListableBeanFactory listableBeanFactory = (ListableBeanFactory) this.beanFactory;
			Map<String, PropertySourcesPlaceholderConfigurer> beans = listableBeanFactory
					.getBeansOfType(PropertySourcesPlaceholderConfigurer.class, false,
							false);
			if (beans.size() == 1) {
				return beans.values().iterator().next();
			}
			if (beans.size() > 1 && logger.isWarnEnabled()) {
				logger.warn("Multiple PropertySourcesPlaceholderConfigurer "
						+ "beans registered " + beans.keySet()
						+ ", falling back to Environment");
			}
		}
		return null;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		ConfigurationProperties annotation = getAnnotation(bean, beanName);
		if (annotation != null) {
			try {
				getBinder().bind(bean, annotation);
			}
			catch (ConfigurationPropertiesBindingException ex) {
				throw new BeanCreationException(beanName, ex.getMessage(), ex.getCause());
			}
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {
		return bean;
	}

	private ConfigurationProperties getAnnotation(Object bean, String beanName) {
		ConfigurationProperties annotation = this.beans.findFactoryAnnotation(beanName,
				ConfigurationProperties.class);
		if (annotation == null) {
			annotation = AnnotationUtils.findAnnotation(bean.getClass(),
					ConfigurationProperties.class);
		}
		return annotation;
	}

	private ConfigurationPropertiesBinder getBinder() {
		if (this.configurationPropertiesBinder == null) {
			this.configurationPropertiesBinder = new ConfigurationPropertiesBinderBuilder(
					this.applicationContext).withPropertySources(this.propertySources)
							.build();
		}
		return this.configurationPropertiesBinder;
	}

}
