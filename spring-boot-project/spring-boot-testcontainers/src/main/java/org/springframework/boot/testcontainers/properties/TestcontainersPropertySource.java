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

	TestcontainersPropertySource() {
		this(Collections.synchronizedMap(new LinkedHashMap<>()));
	}

	private TestcontainersPropertySource(Map<String, Supplier<Object>> valueSuppliers) {
		super(NAME, Collections.unmodifiableMap(valueSuppliers));
		this.registry = (name, valueSupplier) -> {
			Assert.hasText(name, "'name' must not be null or blank");
			Assert.notNull(valueSupplier, "'valueSupplier' must not be null");
			valueSuppliers.put(name, valueSupplier);
		};
	}

	private void addEventPublisher(ApplicationEventPublisher eventPublisher) {
		this.eventPublishers.add(eventPublisher);
	}

	@Override
	public Object getProperty(String name) {
		Object valueSupplier = this.source.get(name);
		return (valueSupplier != null) ? getProperty(name, valueSupplier) : null;
	}

	@SuppressWarnings({ "removal", "deprecation" })
	private Object getProperty(String name, Object valueSupplier) {
		BeforeTestcontainersPropertySuppliedEvent event = new BeforeTestcontainersPropertySuppliedEvent(this, name);
		this.eventPublishers.forEach((eventPublisher) -> eventPublisher.publishEvent(event));
		return SupplierUtils.resolve(valueSupplier);
	}

	public static DynamicPropertyRegistry attach(Environment environment) {
		return attach(environment, null);
	}

	static DynamicPropertyRegistry attach(ConfigurableApplicationContext applicationContext) {
		return attach(applicationContext.getEnvironment(), applicationContext, null);
	}

	public static DynamicPropertyRegistry attach(Environment environment, BeanDefinitionRegistry registry) {
		return attach(environment, null, registry);
	}

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

		EventPublisherRegistrar(Environment environment) {
			this.environment = environment;
		}

		@Override
		public void setApplicationEventPublisher(ApplicationEventPublisher eventPublisher) {
			this.eventPublisher = eventPublisher;
		}

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
			if (this.eventPublisher != null) {
				TestcontainersPropertySource.getOrAdd((ConfigurableEnvironment) this.environment)
					.addEventPublisher(this.eventPublisher);
			}
		}

	}

}
