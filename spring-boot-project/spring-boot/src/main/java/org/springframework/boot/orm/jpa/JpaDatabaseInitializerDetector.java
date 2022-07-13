/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.orm.jpa;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import jakarta.persistence.EntityManagerFactory;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.sql.init.dependency.AbstractBeansOfTypeDatabaseInitializerDetector;
import org.springframework.boot.sql.init.dependency.DatabaseInitializerDetector;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * A {@link DatabaseInitializerDetector} for JPA.
 *
 * @author Andy Wilkinson
 */
class JpaDatabaseInitializerDetector extends AbstractBeansOfTypeDatabaseInitializerDetector {

	private final Environment environment;

	JpaDatabaseInitializerDetector(Environment environment) {
		this.environment = environment;
	}

	@Override
	protected Set<Class<?>> getDatabaseInitializerBeanTypes() {
		boolean deferred = this.environment.getProperty("spring.jpa.defer-datasource-initialization", boolean.class,
				false);
		return deferred ? Collections.singleton(EntityManagerFactory.class) : Collections.emptySet();
	}

	@Override
	public void detectionComplete(ConfigurableListableBeanFactory beanFactory, Set<String> dataSourceInitializerNames) {
		configureOtherInitializersToDependOnJpaInitializers(beanFactory, dataSourceInitializerNames);
	}

	private void configureOtherInitializersToDependOnJpaInitializers(ConfigurableListableBeanFactory beanFactory,
			Set<String> dataSourceInitializerNames) {
		Set<String> jpaInitializers = new HashSet<>();
		Set<String> otherInitializers = new HashSet<>(dataSourceInitializerNames);
		Iterator<String> iterator = otherInitializers.iterator();
		while (iterator.hasNext()) {
			String initializerName = iterator.next();
			BeanDefinition initializerDefinition = beanFactory.getBeanDefinition(initializerName);
			if (JpaDatabaseInitializerDetector.class.getName()
					.equals(initializerDefinition.getAttribute(DatabaseInitializerDetector.class.getName()))) {
				iterator.remove();
				jpaInitializers.add(initializerName);
			}
		}
		for (String otherInitializerName : otherInitializers) {
			BeanDefinition definition = beanFactory.getBeanDefinition(otherInitializerName);
			String[] dependencies = definition.getDependsOn();
			for (String dependencyName : jpaInitializers) {
				dependencies = StringUtils.addStringToArray(dependencies, dependencyName);
			}
			definition.setDependsOn(dependencies);
		}
	}

}
