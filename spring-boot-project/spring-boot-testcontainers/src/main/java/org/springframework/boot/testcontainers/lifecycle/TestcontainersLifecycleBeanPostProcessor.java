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

package org.springframework.boot.testcontainers.lifecycle;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
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
class TestcontainersLifecycleBeanPostProcessor implements DestructionAwareBeanPostProcessor {

	private static final Log logger = LogFactory.getLog(TestcontainersLifecycleBeanPostProcessor.class);

	private final ConfigurableListableBeanFactory beanFactory;

	private final TestcontainersStartup startup;

	private final AtomicBoolean startablesInitialized = new AtomicBoolean();

	private final AtomicBoolean containersInitialized = new AtomicBoolean();

	TestcontainersLifecycleBeanPostProcessor(ConfigurableListableBeanFactory beanFactory,
			TestcontainersStartup startup) {
		this.beanFactory = beanFactory;
		this.startup = startup;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (this.beanFactory.isConfigurationFrozen() && this.containersInitialized.compareAndSet(false, true)) {
			initializeContainers();
		}
		if (bean instanceof Startable startableBean) {
			if (this.startablesInitialized.compareAndSet(false, true)) {
				initializeStartables(startableBean, beanName);
			}
			else {
				startableBean.start();
			}
		}
		return bean;
	}

	private void initializeStartables(Startable startableBean, String startableBeanName) {
		List<String> beanNames = new ArrayList<>(
				List.of(this.beanFactory.getBeanNamesForType(Startable.class, false, false)));
		beanNames.remove(startableBeanName);
		List<Object> beans = getBeans(beanNames);
		if (beans == null) {
			this.startablesInitialized.set(false);
			return;
		}
		beanNames.add(startableBeanName);
		beans.add(startableBean);
		start(beans);
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
		List<String> beanNames = List.of(this.beanFactory.getBeanNamesForType(ContainerState.class, false, false));
		if (getBeans(beanNames) == null) {
			this.containersInitialized.set(false);
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
		return (bean instanceof GenericContainer<?> container) && container.isShouldBeReused();
	}

}
