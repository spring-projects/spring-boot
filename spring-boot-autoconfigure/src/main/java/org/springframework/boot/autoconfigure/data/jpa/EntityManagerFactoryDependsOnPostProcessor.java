/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.jpa;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManagerFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.orm.jpa.AbstractEntityManagerFactoryBean;
import org.springframework.util.StringUtils;

/**
 * {@link BeanFactoryPostProcessor} that can be used to dynamically declare that all
 * {@link EntityManagerFactory} beans should "depend on" a specific bean.
 *
 * @author Marcel Overdijk
 * @author Dave Syer
 * @author Phillip Webb
 * @since 1.1.0
 * @see BeanDefinition#setDependsOn(String[])
 */
public class EntityManagerFactoryDependsOnPostProcessor implements
		BeanFactoryPostProcessor {

	private final String[] dependsOn;

	public EntityManagerFactoryDependsOnPostProcessor(String... dependsOn) {
		this.dependsOn = dependsOn;
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		for (String beanName : getEntityManagerFactoryBeanNames(beanFactory)) {
			BeanDefinition definition = getBeanDefinition(beanName, beanFactory);
			String[] dependencies = definition.getDependsOn();
			for (String bean : this.dependsOn) {
				dependencies = StringUtils.addStringToArray(dependencies, bean);
			}
			definition.setDependsOn(dependencies);
		}
	}

	private static BeanDefinition getBeanDefinition(String beanName,
			ConfigurableListableBeanFactory beanFactory) {
		try {
			return beanFactory.getBeanDefinition(beanName);
		}
		catch (NoSuchBeanDefinitionException ex) {
			BeanFactory parentBeanFactory = beanFactory.getParentBeanFactory();
			if (parentBeanFactory instanceof ConfigurableListableBeanFactory) {
				return getBeanDefinition(beanName,
						(ConfigurableListableBeanFactory) parentBeanFactory);
			}
			throw ex;
		}
	}

	private Iterable<String> getEntityManagerFactoryBeanNames(
			ListableBeanFactory beanFactory) {
		Set<String> names = new HashSet<String>();
		names.addAll(Arrays.asList(BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
				beanFactory, EntityManagerFactory.class, true, false)));
		for (String factoryBeanName : BeanFactoryUtils
				.beanNamesForTypeIncludingAncestors(beanFactory,
						AbstractEntityManagerFactoryBean.class, true, false)) {
			names.add(BeanFactoryUtils.transformedBeanName(factoryBeanName));
		}
		return names;
	}

}
