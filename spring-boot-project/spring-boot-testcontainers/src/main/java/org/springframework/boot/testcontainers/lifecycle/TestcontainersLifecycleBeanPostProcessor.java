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

package org.springframework.boot.testcontainers.lifecycle;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testcontainers.containers.ContainerState;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.lifecycle.Startable;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.boot.testcontainers.properties.BeforeTestcontainersPropertySuppliedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.log.LogMessage;

/**
 * {@link BeanPostProcessor} to manage the lifecycle of {@link Startable startable
 * containers}.
 * <p>
 * As well as starting containers, this {@link BeanPostProcessor} will also ensure that
 * all containers are started as early as possible in the
 * {@link ConfigurableListableBeanFactory#preInstantiateSingletons() pre-instantiate
 * singletons} phase.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @see TestcontainersLifecycleApplicationContextInitializer
 */
@Order(Ordered.LOWEST_PRECEDENCE)
class TestcontainersLifecycleBeanPostProcessor
		implements DestructionAwareBeanPostProcessor, ApplicationListener<BeforeTestcontainersPropertySuppliedEvent> {

	private static final Log logger = LogFactory.getLog(TestcontainersLifecycleBeanPostProcessor.class);

	private final ConfigurableListableBeanFactory beanFactory;

	private final TestcontainersStartup startup;

	private final AtomicReference<Startables> startables = new AtomicReference<>(Startables.UNSTARTED);

	private final AtomicBoolean containersInitialized = new AtomicBoolean();

	/**
	 * Constructs a new TestcontainersLifecycleBeanPostProcessor with the specified bean
	 * factory and startup object.
	 * @param beanFactory the bean factory to be used
	 * @param startup the startup object to be used
	 */
	TestcontainersLifecycleBeanPostProcessor(ConfigurableListableBeanFactory beanFactory,
			TestcontainersStartup startup) {
		this.beanFactory = beanFactory;
		this.startup = startup;
	}

	/**
	 * This method is called when a BeforeTestcontainersPropertySuppliedEvent is
	 * triggered. It initializes the containers for the test environment.
	 */
	@Override
	public void onApplicationEvent(BeforeTestcontainersPropertySuppliedEvent event) {
		initializeContainers();
	}

	/**
	 * This method is called after the initialization of a bean. If the bean factory's
	 * configuration is frozen, it initializes the containers. If the bean is an instance
	 * of Startable, it checks the state of the startables and performs the necessary
	 * actions. If the startables are in the UNSTARTED state, it initializes the
	 * startables. If the startables are in the STARTED state, it starts the container
	 * associated with the bean.
	 * @param bean the initialized bean
	 * @param beanName the name of the bean
	 * @return the initialized bean
	 * @throws BeansException if an error occurs during the initialization process
	 */
	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (this.beanFactory.isConfigurationFrozen()) {
			initializeContainers();
		}
		if (bean instanceof Startable startableBean) {
			if (this.startables.compareAndExchange(Startables.UNSTARTED, Startables.STARTING) == Startables.UNSTARTED) {
				initializeStartables(startableBean, beanName);
			}
			else if (this.startables.get() == Startables.STARTED) {
				logger.trace(LogMessage.format("Starting container %s", beanName));
				startableBean.start();
			}
		}
		return bean;
	}

	/**
	 * Initializes the startables by adding the given startable bean and starting all
	 * other startable beans.
	 * @param startableBean the startable bean to be added and started
	 * @param startableBeanName the name of the startable bean
	 */
	private void initializeStartables(Startable startableBean, String startableBeanName) {
		logger.trace(LogMessage.format("Initializing startables"));
		List<String> beanNames = new ArrayList<>(
				List.of(this.beanFactory.getBeanNamesForType(Startable.class, false, false)));
		beanNames.remove(startableBeanName);
		List<Object> beans = getBeans(beanNames);
		if (beans == null) {
			logger.trace(LogMessage.format("Failed to obtain startables %s", beanNames));
			this.startables.set(Startables.UNSTARTED);
			return;
		}
		beanNames.add(startableBeanName);
		beans.add(startableBean);
		logger.trace(LogMessage.format("Starting startables %s", beanNames));
		start(beans);
		this.startables.set(Startables.STARTED);
		if (!beanNames.isEmpty()) {
			logger.debug(LogMessage.format("Initialized and started startable beans '%s'", beanNames));
		}
	}

	/**
	 * Starts the given list of beans that implement the Startable interface.
	 * @param beans the list of beans to start
	 */
	private void start(List<Object> beans) {
		Set<Startable> startables = beans.stream()
			.filter(Startable.class::isInstance)
			.map(Startable.class::cast)
			.collect(Collectors.toCollection(LinkedHashSet::new));
		this.startup.start(startables);
	}

	/**
	 * Initializes the containers.
	 *
	 * This method is responsible for initializing the containers if they have not been
	 * initialized already. It checks if the containers have been initialized by using the
	 * compareAndSet method on the containersInitialized variable. If the containers have
	 * not been initialized, it logs a trace message and proceeds with the initialization
	 * process.
	 *
	 * The initialization process involves retrieving the bean names of the ContainerState
	 * class from the bean factory, and then retrieving the corresponding beans using the
	 * getBeans method. If the beans are successfully retrieved, a trace message is logged
	 * indicating the successful initialization of the containers. Otherwise, a trace
	 * message is logged indicating the failure to initialize the containers, and the
	 * containersInitialized variable is set to false again.
	 *
	 * @see TestcontainersLifecycleBeanPostProcessor
	 */
	private void initializeContainers() {
		if (this.containersInitialized.compareAndSet(false, true)) {
			logger.trace("Initializing containers");
			List<String> beanNames = List.of(this.beanFactory.getBeanNamesForType(ContainerState.class, false, false));
			List<Object> beans = getBeans(beanNames);
			if (beans != null) {
				logger.trace(LogMessage.format("Initialized containers %s", beanNames));
			}
			else {
				logger.trace(LogMessage.format("Failed to initialize containers %s", beanNames));
				this.containersInitialized.set(false);
			}
		}
	}

	/**
	 * Retrieves a list of beans based on the given list of bean names.
	 * @param beanNames the list of bean names to retrieve beans for
	 * @return a list of beans corresponding to the given bean names, or null if a bean is
	 * currently being created
	 * @throws BeanCreationException if an error occurs while creating a bean
	 */
	private List<Object> getBeans(List<String> beanNames) {
		List<Object> beans = new ArrayList<>(beanNames.size());
		for (String beanName : beanNames) {
			try {
				beans.add(this.beanFactory.getBean(beanName));
			}
			catch (BeanCreationException ex) {
				if (ex.contains(BeanCurrentlyInCreationException.class)) {
					return null;
				}
				throw ex;
			}
		}
		return beans;
	}

	/**
	 * Determines if the given bean requires destruction.
	 * @param bean the bean to check
	 * @return true if the bean requires destruction, false otherwise
	 */
	@Override
	public boolean requiresDestruction(Object bean) {
		return bean instanceof Startable;
	}

	/**
	 * This method is called before the destruction of a bean. It checks if the bean is an
	 * instance of Startable and if it has not been destroyed by the framework or reused
	 * by the container. If these conditions are met, it calls the close() method on the
	 * Startable bean to perform any necessary cleanup or shutdown operations.
	 * @param bean The bean object being processed.
	 * @param beanName The name of the bean being processed.
	 * @throws BeansException If an error occurs during the bean processing.
	 */
	@Override
	public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
		if (bean instanceof Startable startable && !isDestroyedByFramework(beanName) && !isReusedContainer(bean)) {
			startable.close();
		}
	}

	/**
	 * Checks if a bean is destroyed by the framework.
	 * @param beanName the name of the bean to check
	 * @return true if the bean is destroyed by the framework, false otherwise
	 */
	private boolean isDestroyedByFramework(String beanName) {
		try {
			BeanDefinition beanDefinition = this.beanFactory.getBeanDefinition(beanName);
			String destroyMethodName = beanDefinition.getDestroyMethodName();
			return !"".equals(destroyMethodName);
		}
		catch (NoSuchBeanDefinitionException ex) {
			return false;
		}
	}

	/**
	 * Checks if the given bean is a reusable container.
	 * @param bean the object to be checked
	 * @return true if the bean is a reusable container, false otherwise
	 */
	private boolean isReusedContainer(Object bean) {
		return (bean instanceof GenericContainer<?> container) && container.isShouldBeReused();
	}

	enum Startables {

		UNSTARTED, STARTING, STARTED

	}

}
