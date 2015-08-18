/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.jta.atomikos;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.Ordered;

import com.atomikos.icatch.jta.UserTransactionManager;

/**
 * {@link BeanFactoryPostProcessor} to automatically setup the recommended
 * {@link BeanDefinition#setDependsOn(String[]) dependsOn} settings for <a
 * href="http://www.atomikos.com/Documentation/SpringIntegration">correct Atomikos
 * ordering</a>.
 *
 * @author Phillip Webb
 * @since 1.2.0
 */
public class AtomikosDependsOnBeanFactoryPostProcessor implements
		BeanFactoryPostProcessor, Ordered {

	private static final String[] NO_BEANS = {};

	private int order = Ordered.LOWEST_PRECEDENCE;

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
			throws BeansException {
		String[] transactionManagers = beanFactory.getBeanNamesForType(
				UserTransactionManager.class, true, false);
		for (String transactionManager : transactionManagers) {
			addTransactionManagerDependencies(beanFactory, transactionManager);
		}
		addMessageDrivenContainerDependencies(beanFactory, transactionManagers);
	}

	private void addTransactionManagerDependencies(
			ConfigurableListableBeanFactory beanFactory, String transactionManager) {
		BeanDefinition bean = beanFactory.getBeanDefinition(transactionManager);
		Set<String> dependsOn = new LinkedHashSet<String>(asList(bean.getDependsOn()));
		int initialSize = dependsOn.size();
		addDependencies(beanFactory, "javax.jms.ConnectionFactory", dependsOn);
		addDependencies(beanFactory, "javax.sql.DataSource", dependsOn);
		if (dependsOn.size() != initialSize) {
			bean.setDependsOn(dependsOn.toArray(new String[dependsOn.size()]));
		}
	}

	private void addMessageDrivenContainerDependencies(
			ConfigurableListableBeanFactory beanFactory, String[] transactionManagers) {
		String[] messageDrivenContainers = getBeanNamesForType(beanFactory,
				"com.atomikos.jms.extra.MessageDrivenContainer");
		for (String messageDrivenContainer : messageDrivenContainers) {
			BeanDefinition bean = beanFactory.getBeanDefinition(messageDrivenContainer);
			Set<String> dependsOn = new LinkedHashSet<String>(asList(bean.getDependsOn()));
			dependsOn.addAll(asList(transactionManagers));
			bean.setDependsOn(dependsOn.toArray(new String[dependsOn.size()]));
		}
	}

	private void addDependencies(ConfigurableListableBeanFactory beanFactory,
			String type, Set<String> dependsOn) {
		dependsOn.addAll(asList(getBeanNamesForType(beanFactory, type)));
	}

	private String[] getBeanNamesForType(ConfigurableListableBeanFactory beanFactory,
			String type) {
		try {
			return beanFactory.getBeanNamesForType(Class.forName(type), true, false);
		}
		catch (ClassNotFoundException ex) {
			// Ignore
		}
		catch (NoClassDefFoundError ex) {
			// Ignore
		}
		return NO_BEANS;
	}

	private List<String> asList(String[] array) {
		return (array == null ? Collections.<String> emptyList() : Arrays.asList(array));
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

}
