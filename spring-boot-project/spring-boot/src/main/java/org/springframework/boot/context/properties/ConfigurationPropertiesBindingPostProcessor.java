/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.properties;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.context.properties.bind.BindMethod;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.PropertySources;
import org.springframework.util.Assert;

/**
 * {@link BeanPostProcessor} to bind {@link PropertySources} to beans annotated with
 * {@link ConfigurationProperties @ConfigurationProperties}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Christian Dupuis
 * @author Stephane Nicoll
 * @author Madhura Bhave
 * @since 1.0.0
 */
public class ConfigurationPropertiesBindingPostProcessor
		implements BeanPostProcessor, PriorityOrdered, ApplicationContextAware, InitializingBean {

	/**
	 * The bean name that this post-processor is registered with.
	 */
	public static final String BEAN_NAME = ConfigurationPropertiesBindingPostProcessor.class.getName();

	private ApplicationContext applicationContext;

	private BeanDefinitionRegistry registry;

	private ConfigurationPropertiesBinder binder;

	/**
     * Set the application context that this object runs in.
     * 
     * @param applicationContext the application context to be set
     * @throws BeansException if an error occurs while setting the application context
     */
    @Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	/**
     * Callback method invoked by the container after all bean properties have been set.
     * Initializes the registry and binder properties.
     *
     * @throws Exception if an error occurs during initialization
     */
    @Override
	public void afterPropertiesSet() throws Exception {
		// We can't use constructor injection of the application context because
		// it causes eager factory bean initialization
		this.registry = (BeanDefinitionRegistry) this.applicationContext.getAutowireCapableBeanFactory();
		this.binder = ConfigurationPropertiesBinder.get(this.applicationContext);
	}

	/**
     * Returns the order of this ConfigurationPropertiesBindingPostProcessor.
     * The order is determined by adding 1 to the highest precedence value.
     * 
     * @return the order of this ConfigurationPropertiesBindingPostProcessor
     */
    @Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE + 1;
	}

	/**
     * This method is called before the initialization of a bean. It checks if the bean has a bound value object associated with it.
     * If not, it binds the bean with the corresponding ConfigurationPropertiesBean using the ApplicationContext.
     * 
     * @param bean The bean object being processed.
     * @param beanName The name of the bean being processed.
     * @return The processed bean object.
     * @throws BeansException If an error occurs during the bean processing.
     */
    @Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (!hasBoundValueObject(beanName)) {
			bind(ConfigurationPropertiesBean.get(this.applicationContext, bean, beanName));
		}
		return bean;
	}

	/**
     * Checks if the specified bean has a bound value object.
     * 
     * @param beanName the name of the bean to check
     * @return true if the bean has a bound value object, false otherwise
     */
    private boolean hasBoundValueObject(String beanName) {
		return BindMethod.VALUE_OBJECT.equals(BindMethodAttribute.get(this.registry, beanName));
	}

	/**
     * Binds the given ConfigurationPropertiesBean to its corresponding configuration properties.
     * 
     * @param bean the ConfigurationPropertiesBean to bind
     * @throws ConfigurationPropertiesBindException if an error occurs during the binding process
     * @throws IllegalStateException if @ConstructorBinding has been applied to a regular bean
     */
    private void bind(ConfigurationPropertiesBean bean) {
		if (bean == null) {
			return;
		}
		Assert.state(bean.asBindTarget().getBindMethod() != BindMethod.VALUE_OBJECT,
				"Cannot bind @ConfigurationProperties for bean '" + bean.getName()
						+ "'. Ensure that @ConstructorBinding has not been applied to regular bean");
		try {
			this.binder.bind(bean);
		}
		catch (Exception ex) {
			throw new ConfigurationPropertiesBindException(bean, ex);
		}
	}

	/**
	 * Register a {@link ConfigurationPropertiesBindingPostProcessor} bean if one is not
	 * already registered.
	 * @param registry the bean definition registry
	 * @since 2.2.0
	 */
	public static void register(BeanDefinitionRegistry registry) {
		Assert.notNull(registry, "Registry must not be null");
		if (!registry.containsBeanDefinition(BEAN_NAME)) {
			BeanDefinition definition = BeanDefinitionBuilder
				.rootBeanDefinition(ConfigurationPropertiesBindingPostProcessor.class)
				.getBeanDefinition();
			definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			registry.registerBeanDefinition(BEAN_NAME, definition);
		}
		ConfigurationPropertiesBinder.register(registry);
	}

}
