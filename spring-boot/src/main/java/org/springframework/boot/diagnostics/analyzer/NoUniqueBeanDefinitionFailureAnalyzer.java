/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.diagnostics.analyzer;

import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * An {@link AbstractFailureAnalyzer} that performs analysis of failures caused by a
 * {@link NoUniqueBeanDefinitionException}.
 *
 * @author Andy Wilkinson
 */
class NoUniqueBeanDefinitionFailureAnalyzer
		extends AbstractFailureAnalyzer<NoUniqueBeanDefinitionException>
		implements BeanFactoryAware {

	private ConfigurableBeanFactory beanFactory;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		Assert.isInstanceOf(ConfigurableBeanFactory.class, beanFactory);
		this.beanFactory = (ConfigurableBeanFactory) beanFactory;
	}

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure,
			NoUniqueBeanDefinitionException cause) {
		String consumerDescription = getConsumerDescription(rootFailure);
		if (consumerDescription == null) {
			return null;
		}
		String[] beanNames = extractBeanNames(cause);
		if (beanNames == null) {
			return null;
		}
		StringBuilder message = new StringBuilder();
		message.append(String.format("%s required a single bean, but %d were found:%n",
				consumerDescription, beanNames.length));
		for (String beanName : beanNames) {
			try {
				BeanDefinition beanDefinition = this.beanFactory
						.getMergedBeanDefinition(beanName);
				if (StringUtils.hasText(beanDefinition.getFactoryMethodName())) {
					message.append(String.format("\t- %s: defined by method '%s' in %s%n",
							beanName, beanDefinition.getFactoryMethodName(),
							beanDefinition.getResourceDescription()));
				}
				else {
					message.append(String.format("\t- %s: defined in %s%n", beanName,
							beanDefinition.getResourceDescription()));
				}
			}
			catch (NoSuchBeanDefinitionException ex) {
				message.append(String.format(
						"\t- %s: a programmatically registered singleton", beanName));
			}

		}
		return new FailureAnalysis(message.toString(),
				"Consider marking one of the beans as @Primary, updating the consumer to"
						+ " accept multiple beans, or using @Qualifier to identify the"
						+ " bean that should be consumed",
				cause);
	}

	private String getConsumerDescription(Throwable ex) {
		UnsatisfiedDependencyException unsatisfiedDependency = findUnsatisfiedDependencyException(
				ex);
		if (unsatisfiedDependency != null) {
			return getConsumerDescription(unsatisfiedDependency);
		}
		BeanInstantiationException beanInstantiationException = findBeanInstantiationException(
				ex);
		if (beanInstantiationException != null) {
			return getConsumerDescription(beanInstantiationException);
		}
		return null;
	}

	private UnsatisfiedDependencyException findUnsatisfiedDependencyException(
			Throwable root) {
		return findMostNestedCause(root, UnsatisfiedDependencyException.class);
	}

	private BeanInstantiationException findBeanInstantiationException(Throwable root) {
		return findMostNestedCause(root, BeanInstantiationException.class);
	}

	@SuppressWarnings("unchecked")
	private <T extends Exception> T findMostNestedCause(Throwable root,
			Class<T> causeType) {
		Throwable candidate = root;
		T mostNestedMatch = null;
		while (candidate != null) {
			if (causeType.isAssignableFrom(candidate.getClass())) {
				mostNestedMatch = (T) candidate;
			}
			candidate = candidate.getCause();
		}
		return mostNestedMatch;
	}

	private String getConsumerDescription(UnsatisfiedDependencyException ex) {
		InjectionPoint injectionPoint = ex.getInjectionPoint();
		if (injectionPoint != null) {
			if (injectionPoint.getField() != null) {
				return String.format("Field %s in %s",
						injectionPoint.getField().getName(),
						injectionPoint.getField().getDeclaringClass().getName());
			}
			if (injectionPoint.getMethodParameter() != null) {
				if (injectionPoint.getMethodParameter().getConstructor() != null) {
					return String.format("Parameter %d of constructor in %s",
							injectionPoint.getMethodParameter().getParameterIndex(),
							injectionPoint.getMethodParameter().getDeclaringClass()
									.getName());
				}
				return String.format("Parameter %d of method %s in %s",
						injectionPoint.getMethodParameter().getParameterIndex(),
						injectionPoint.getMethodParameter().getMethod().getName(),
						injectionPoint.getMethodParameter().getDeclaringClass()
								.getName());
			}
		}
		return ex.getResourceDescription();
	}

	private String getConsumerDescription(BeanInstantiationException ex) {
		if (ex.getConstructingMethod() != null) {
			return String.format("Method %s in %s", ex.getConstructingMethod().getName(),
					ex.getConstructingMethod().getDeclaringClass().getName());
		}
		if (ex.getConstructor() != null) {
			return String.format("Constructor in %s", ClassUtils
					.getUserClass(ex.getConstructor().getDeclaringClass()).getName());
		}
		return ex.getBeanClass().getName();
	}

	private String[] extractBeanNames(NoUniqueBeanDefinitionException cause) {
		if (cause.getMessage().indexOf("but found") > -1) {
			return StringUtils.commaDelimitedListToStringArray(cause.getMessage()
					.substring(cause.getMessage().lastIndexOf(":") + 1).trim());
		}
		return null;
	}

}
