/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.testcontainers.properties;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Supplier;

import org.testcontainers.containers.Container;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.util.Assert;
import org.springframework.util.function.SupplierUtils;

/**
 * {@link EnumerablePropertySource} backed by a map with values supplied from one or more
 * {@link Container testcontainers}.
 *
 * @author Phillip Webb
 * @since 3.1.0
 */
public class TestcontainersPropertySource extends MapPropertySource {

	static final String NAME = "testcontainersPropertySource";

	private final DynamicPropertyRegistry registry;

	private final Set<ApplicationEventPublisher> eventPublishers = new CopyOnWriteArraySet<>();

	/**
     * Constructs a new TestcontainersPropertySource with the specified map.
     *
     * @param map the map to be used for storing properties
     */
    TestcontainersPropertySource() {
		this(Collections.synchronizedMap(new LinkedHashMap<>()));
	}

	/**
     * Constructs a new TestcontainersPropertySource with the given value suppliers.
     *
     * @param valueSuppliers the map of value suppliers for the property source
     * @throws IllegalArgumentException if the name or value supplier is null or blank
     */
    private TestcontainersPropertySource(Map<String, Supplier<Object>> valueSuppliers) {
		super(NAME, Collections.unmodifiableMap(valueSuppliers));
		this.registry = (name, valueSupplier) -> {
			Assert.hasText(name, "'name' must not be null or blank");
			Assert.notNull(valueSupplier, "'valueSupplier' must not be null");
			valueSuppliers.put(name, valueSupplier);
		};
	}

	/**
     * Adds an ApplicationEventPublisher to the list of event publishers.
     * 
     * @param eventPublisher the ApplicationEventPublisher to be added
     */
    private void addEventPublisher(ApplicationEventPublisher eventPublisher) {
		this.eventPublishers.add(eventPublisher);
	}

	/**
     * Retrieves the value of the specified property.
     *
     * @param name the name of the property to retrieve
     * @return the value of the property, or null if the property does not exist
     */
    @Override
	public Object getProperty(String name) {
		Object valueSupplier = this.source.get(name);
		return (valueSupplier != null) ? getProperty(name, valueSupplier) : null;
	}

	/**
     * Retrieves the value of a property based on its name and value supplier.
     * 
     * @param name the name of the property
     * @param valueSupplier the supplier that provides the value of the property
     * @return the value of the property
     */
    private Object getProperty(String name, Object valueSupplier) {
		BeforeTestcontainersPropertySuppliedEvent event = new BeforeTestcontainersPropertySuppliedEvent(this, name);
		this.eventPublishers.forEach((eventPublisher) -> eventPublisher.publishEvent(event));
		return SupplierUtils.resolve(valueSupplier);
	}

	/**
     * Attaches a dynamic property registry to the specified environment.
     *
     * @param environment the environment to attach the dynamic property registry to
     * @return the attached dynamic property registry
     */
    public static DynamicPropertyRegistry attach(Environment environment) {
		return attach(environment, null);
	}

	/**
     * Attaches a dynamic property registry to the given ConfigurableApplicationContext.
     * 
     * @param applicationContext the ConfigurableApplicationContext to attach the dynamic property registry to
     * @return the attached DynamicPropertyRegistry
     */
    static DynamicPropertyRegistry attach(ConfigurableApplicationContext applicationContext) {
		return attach(applicationContext.getEnvironment(), applicationContext, null);
	}

	/**
     * Attaches a dynamic property registry to the given environment and bean definition registry.
     * 
     * @param environment the environment to attach the dynamic property registry to
     * @param registry the bean definition registry to attach the dynamic property registry to
     * @return the attached dynamic property registry
     */
    public static DynamicPropertyRegistry attach(Environment environment, BeanDefinitionRegistry registry) {
		return attach(environment, null, registry);
	}

	/**
     * Attaches the TestcontainersPropertySource to the given environment, event publisher, and bean definition registry.
     * 
     * @param environment The environment to attach the TestcontainersPropertySource to. Must be a ConfigurableEnvironment.
     * @param eventPublisher The event publisher to add to the TestcontainersPropertySource. Can be null.
     * @param registry The bean definition registry to register the EventPublisherRegistrar bean definition. Can be null.
     * @return The DynamicPropertyRegistry associated with the TestcontainersPropertySource.
     * @throws IllegalStateException if the environment is not a ConfigurableEnvironment.
     */
    private static DynamicPropertyRegistry attach(Environment environment, ApplicationEventPublisher eventPublisher,
			BeanDefinitionRegistry registry) {
		Assert.state(environment instanceof ConfigurableEnvironment,
				"TestcontainersPropertySource can only be attached to a ConfigurableEnvironment");
		TestcontainersPropertySource propertySource = getOrAdd((ConfigurableEnvironment) environment);
		if (eventPublisher != null) {
			propertySource.addEventPublisher(eventPublisher);
		}
		else if (registry != null && !registry.containsBeanDefinition(EventPublisherRegistrar.NAME)) {
			registry.registerBeanDefinition(EventPublisherRegistrar.NAME, new RootBeanDefinition(
					EventPublisherRegistrar.class, () -> new EventPublisherRegistrar(environment)));
		}
		return propertySource.registry;
	}

	/**
     * Retrieves the TestcontainersPropertySource from the given ConfigurableEnvironment, or adds it if it does not exist.
     * 
     * @param environment the ConfigurableEnvironment from which to retrieve or add the TestcontainersPropertySource
     * @return the TestcontainersPropertySource
     * @throws IllegalStateException if the TestcontainersPropertySource type registered is incorrect
     */
    static TestcontainersPropertySource getOrAdd(ConfigurableEnvironment environment) {
		PropertySource<?> propertySource = environment.getPropertySources().get(NAME);
		if (propertySource == null) {
			environment.getPropertySources().addFirst(new TestcontainersPropertySource());
			return getOrAdd(environment);
		}
		Assert.state(propertySource instanceof TestcontainersPropertySource,
				"Incorrect TestcontainersPropertySource type registered");
		return ((TestcontainersPropertySource) propertySource);
	}

	/**
	 * {@link BeanFactoryPostProcessor} to register the {@link ApplicationEventPublisher}
	 * to the {@link TestcontainersPropertySource}. This class is a
	 * {@link BeanFactoryPostProcessor} so that it is initialized as early as possible.
	 */
	static class EventPublisherRegistrar implements BeanFactoryPostProcessor, ApplicationEventPublisherAware {

		static final String NAME = EventPublisherRegistrar.class.getName();

		private final Environment environment;

		private ApplicationEventPublisher eventPublisher;

		/**
         * Constructs a new EventPublisherRegistrar with the specified environment.
         * 
         * @param environment the environment in which the EventPublisherRegistrar operates
         */
        EventPublisherRegistrar(Environment environment) {
			this.environment = environment;
		}

		/**
         * Sets the application event publisher.
         * 
         * @param eventPublisher the application event publisher to be set
         */
        @Override
		public void setApplicationEventPublisher(ApplicationEventPublisher eventPublisher) {
			this.eventPublisher = eventPublisher;
		}

		/**
         * Post-processes the bean factory by adding the event publisher to the Testcontainers property source.
         * 
         * @param beanFactory the bean factory to be processed
         * @throws BeansException if an error occurs during bean processing
         */
        @Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
			if (this.eventPublisher != null) {
				TestcontainersPropertySource.getOrAdd((ConfigurableEnvironment) this.environment)
					.addEventPublisher(this.eventPublisher);
			}
		}

	}

}
