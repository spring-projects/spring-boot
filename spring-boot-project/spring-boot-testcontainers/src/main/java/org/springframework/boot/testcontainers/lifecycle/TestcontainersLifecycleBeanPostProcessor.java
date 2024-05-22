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
import org.testcontainers.utility.TestcontainersConfiguration;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
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
 * @author Scott Frederick
 * @see TestcontainersLifecycleApplicationContextInitializer
 */
@Order(Ordered.LOWEST_PRECEDENCE)
class TestcontainersLifecycleBeanPostProcessor
		implements DestructionAwareBeanPostProcessor, ApplicationListener<BeforeTestcontainerUsedEvent> {

	private static final Log logger = LogFactory.getLog(TestcontainersLifecycleBeanPostProcessor.class);

	private final ConfigurableListableBeanFactory beanFactory;

	private final TestcontainersStartup startup;

	private final AtomicReference<Startables> startables = new AtomicReference<>(Startables.UNSTARTED);

	private final AtomicBoolean containersInitialized = new AtomicBoolean();

	TestcontainersLifecycleBeanPostProcessor(ConfigurableListableBeanFactory beanFactory,
			TestcontainersStartup startup) {
		this.beanFactory = beanFactory;
		this.startup = startup;
	}

	@Override
	public void onApplicationEvent(BeforeTestcontainerUsedEvent event) {
		initializeContainers();
	}

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

	private void start(List<Object> beans) {
		Set<Startable> startables = beans.stream()
			.filter(Startable.class::isInstance)
			.map(Startable.class::cast)
			.collect(Collectors.toCollection(LinkedHashSet::new));
		this.startup.start(startables);
	}

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

	@Override
	public boolean requiresDestruction(Object bean) {
		return bean instanceof Startable;
	}

	@Override
	public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
		if (bean instanceof Startable startable && !isDestroyedByFramework(beanName) && !isReusedContainer(bean)) {
			startable.close();
		}
	}

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

	private boolean isReusedContainer(Object bean) {
		return (bean instanceof GenericContainer<?> container) && container.isShouldBeReused()
				&& TestcontainersConfiguration.getInstance().environmentSupportsReuse();
	}

	enum Startables {

		UNSTARTED, STARTING, STARTED

	}

}
