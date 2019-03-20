/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.jta.narayana;

import javax.transaction.TransactionManager;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.Ordered;

/**
 * {@link BeanFactoryPostProcessor} to automatically setup correct beans ordering.
 *
 * @author Gytis Trikleris
 * @since 1.4.0
 */
public class NarayanaBeanFactoryPostProcessor
		implements BeanFactoryPostProcessor, Ordered {

	private static final String[] NO_BEANS = {};

	private static final int ORDER = Ordered.LOWEST_PRECEDENCE;

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
			throws BeansException {
		String[] transactionManagers = beanFactory
				.getBeanNamesForType(TransactionManager.class, true, false);
		String[] recoveryManagers = beanFactory
				.getBeanNamesForType(NarayanaRecoveryManagerBean.class, true, false);
		addBeanDependencies(beanFactory, transactionManagers, "javax.sql.DataSource");
		addBeanDependencies(beanFactory, recoveryManagers, "javax.sql.DataSource");
		addBeanDependencies(beanFactory, transactionManagers,
				"javax.jms.ConnectionFactory");
		addBeanDependencies(beanFactory, recoveryManagers, "javax.jms.ConnectionFactory");
	}

	private void addBeanDependencies(ConfigurableListableBeanFactory beanFactory,
			String[] beanNames, String dependencyType) {
		for (String beanName : beanNames) {
			addBeanDependencies(beanFactory, beanName, dependencyType);
		}
	}

	private void addBeanDependencies(ConfigurableListableBeanFactory beanFactory,
			String beanName, String dependencyType) {
		for (String dependentBeanName : getBeanNamesForType(beanFactory,
				dependencyType)) {
			beanFactory.registerDependentBean(beanName, dependentBeanName);
		}
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

	@Override
	public int getOrder() {
		return ORDER;
	}

}
