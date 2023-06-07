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

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import org.testcontainers.lifecycle.Startable;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * {@link ApplicationContextInitializer} to manage the lifecycle of {@link Startable
 * startable containers}.
 *
 * @author Phillip Webb
 * @since 3.1.0
 */
public class TestcontainersLifecycleApplicationContextInitializer
		implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	private static final Set<ConfigurableApplicationContext> applied = Collections.newSetFromMap(new WeakHashMap<>());

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		synchronized (applied) {
			if (!applied.add(applicationContext)) {
				return;
			}
		}
		ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
		applicationContext.addBeanFactoryPostProcessor(new TestcontainersLifecycleBeanFactoryPostProcessor());
		beanFactory.addBeanPostProcessor(new TestcontainersLifecycleBeanPostProcessor(beanFactory));
	}

}
