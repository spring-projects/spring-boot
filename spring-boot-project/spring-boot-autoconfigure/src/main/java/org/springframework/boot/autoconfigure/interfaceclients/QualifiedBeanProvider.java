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

package org.springframework.boot.autoconfigure.interfaceclients;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Utility class containing methods that allow searching for beans with a specific
 * qualifier, falling back to the
 * {@link InterfaceClientsAdapter#INTERFACE_CLIENTS_DEFAULT_QUALIFIER} qualifier.
 *
 * @author Josh Long
 * @author Olga Maciaszek-Sharma
 * @since 3.4.0
 */
public final class QualifiedBeanProvider {

	private QualifiedBeanProvider() {
		throw new UnsupportedOperationException("Do not instantiate utility class");
	}

	private static final Log logger = LogFactory.getLog(QualifiedBeanProvider.class);

	public static <T> T qualifiedBean(ListableBeanFactory beanFactory, Class<T> type, String clientId) {
		Map<String, T> matchingClientBeans = getQualifiedBeansOfType(beanFactory, type, clientId);
		if (matchingClientBeans.size() > 1) {
			throw new NoUniqueBeanDefinitionException(type, matchingClientBeans.keySet());
		}
		if (matchingClientBeans.isEmpty()) {
			if (logger.isDebugEnabled()) {
				logger.debug("No qualified bean of type " + type + " found for " + clientId);
			}
			Map<String, T> matchingDefaultBeans = getQualifiedBeansOfType(beanFactory, type,
					org.springframework.boot.autoconfigure.interfaceclients.InterfaceClientsAdapter.INTERFACE_CLIENTS_DEFAULT_QUALIFIER);
			if (matchingDefaultBeans.size() > 1) {
				throw new NoUniqueBeanDefinitionException(type, matchingDefaultBeans.keySet());
			}
			if (matchingDefaultBeans.isEmpty()) {
				if (logger.isDebugEnabled()) {
					logger.debug("No qualified bean of type " + type + " found for default id");
				}
				return null;
			}
		}
		return matchingClientBeans.values().iterator().next();
	}

	private static <T> Map<String, T> getQualifiedBeansOfType(ListableBeanFactory beanFactory, Class<T> type,
			String clientId) {
		Map<String, T> beansOfType = BeanFactoryUtils.beansOfTypeIncludingAncestors(beanFactory, type);
		Map<String, T> matchingClientBeans = new HashMap<>();
		for (String beanName : beansOfType.keySet()) {
			Qualifier qualifier = (beanFactory.findAnnotationOnBean(beanName, Qualifier.class));
			if (qualifier != null && clientId.equals(qualifier.value())) {
				matchingClientBeans.put(beanName, beanFactory.getBean(beanName, type));
			}
		}
		return matchingClientBeans;
	}

}
