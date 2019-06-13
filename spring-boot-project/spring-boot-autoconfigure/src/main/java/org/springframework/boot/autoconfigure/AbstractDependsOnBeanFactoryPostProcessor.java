/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.util.StringUtils;

/**
 * Abstract base class for a {@link BeanFactoryPostProcessor} that can be used to
 * dynamically declare that all beans of a specific type should depend on one or more
 * specific beans.
 *
 * @author Marcel Overdijk
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.3.0
 * @see BeanDefinition#setDependsOn(String[])
 */
public abstract class AbstractDependsOnBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

	private final Class<?> beanClass;

	private final Class<? extends FactoryBean<?>> factoryBeanClass;

	private final String[] dependsOn;

	protected AbstractDependsOnBeanFactoryPostProcessor(Class<?> beanClass,
			Class<? extends FactoryBean<?>> factoryBeanClass, String... dependsOn) {
		this.beanClass = beanClass;
		this.factoryBeanClass = factoryBeanClass;
		this.dependsOn = dependsOn;
	}

	/**
	 * Create an instance with target bean class and dependencies.
	 * @param beanClass target bean class
	 * @param dependsOn dependencies
	 * @since 2.0.4
	 */
	protected AbstractDependsOnBeanFactoryPostProcessor(Class<?> beanClass, String... dependsOn) {
		this(beanClass, null, dependsOn);
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		for (String beanName : getBeanNames(beanFactory)) {
			BeanDefinition definition = getBeanDefinition(beanName, beanFactory);
			String[] dependencies = definition.getDependsOn();
			for (String bean : this.dependsOn) {
				dependencies = StringUtils.addStringToArray(dependencies, bean);
			}
			definition.setDependsOn(dependencies);
		}
	}

	private Iterable<String> getBeanNames(ListableBeanFactory beanFactory) {
		Set<String> names = new HashSet<>();
		names.addAll(Arrays
				.asList(BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, this.beanClass, true, false)));
		if (this.factoryBeanClass != null) {
			for (String factoryBeanName : BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory,
					this.factoryBeanClass, true, false)) {
				names.add(BeanFactoryUtils.transformedBeanName(factoryBeanName));
			}
		}
		return names;
	}

	private static BeanDefinition getBeanDefinition(String beanName, ConfigurableListableBeanFactory beanFactory) {
		try {
			return beanFactory.getBeanDefinition(beanName);
		}
		catch (NoSuchBeanDefinitionException ex) {
			BeanFactory parentBeanFactory = beanFactory.getParentBeanFactory();
			if (parentBeanFactory instanceof ConfigurableListableBeanFactory) {
				return getBeanDefinition(beanName, (ConfigurableListableBeanFactory) parentBeanFactory);
			}
			throw ex;
		}
	}

}
