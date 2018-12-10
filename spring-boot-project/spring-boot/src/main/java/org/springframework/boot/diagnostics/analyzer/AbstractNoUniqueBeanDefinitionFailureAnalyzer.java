/*
 * Copyright 2012-2018 the original author or authors.
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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Abstract base class for a {@link AbstractInjectionFailureAnalyzer} that inspects
 * registered beans for provided names.
 *
 * @param <T> the type of exception to analyze
 * @author Andy Wilkinson
 * @author Dmytro Nosan
 */
public abstract class AbstractNoUniqueBeanDefinitionFailureAnalyzer<T extends Throwable>
		extends AbstractInjectionFailureAnalyzer<T> implements BeanFactoryAware {

	private ConfigurableBeanFactory beanFactory;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		Assert.isInstanceOf(ConfigurableBeanFactory.class, beanFactory);
		this.beanFactory = (ConfigurableBeanFactory) beanFactory;
	}

	@Override
	protected final FailureAnalysis analyze(Throwable rootFailure, T cause,
			String injectionPointDescription) {
		Collection<String> beanNames = getBeanNames(rootFailure, cause);
		if (CollectionUtils.isEmpty(beanNames)) {
			return null;
		}
		List<BeanReport> beanReports = beanNames.stream().map(this::getBeanReport)
				.collect(Collectors.toList());

		return analyze(rootFailure, cause, injectionPointDescription,
				new ConfigurationReport(beanReports));
	}

	/**
	 * Returns an analysis of the given {@code rootFailure}, or {@code null} if no
	 * analysis was possible.
	 * @param rootFailure the root failure passed to the analyzer
	 * @param cause the actual found cause
	 * @param injectionPointDescription the description of the injection point or
	 * {@code null}
	 * @param configurationReport the configuration report.
	 * @return the analysis or {@code null}
	 */

	protected abstract FailureAnalysis analyze(Throwable rootFailure, T cause,
			String injectionPointDescription, ConfigurationReport configurationReport);

	/**
	 * Returns list of the bean names of the given {@code rootFailure} and {@code
	 * cause}, or {@code null} if no analysis was possible.
	 * @param rootFailure the root failure passed to the analyzer
	 * @param cause the actual found cause
	 * @return the bean metadata or {@code null}
	 **/
	protected abstract Collection<String> getBeanNames(Throwable rootFailure, T cause);

	private BeanReport getBeanReport(String name) {
		try {

			BeanDefinition bd = this.beanFactory.getMergedBeanDefinition(name);
			return new BeanReport(name, bd);
		}
		catch (NoSuchBeanDefinitionException ex) {
			return new BeanReport(name, null);
		}
	}

	/**
	 * A configuration report.
	 */
	protected static final class ConfigurationReport {

		private final List<BeanReport> beanReports;

		public ConfigurationReport(List<BeanReport> beanReports) {
			this.beanReports = Collections.unmodifiableList(beanReports);
		}

		/**
		 * Return the bean reports.
		 * @return the list of {@link BeanReport}
		 */
		public List<BeanReport> getBeanReports() {
			return this.beanReports;
		}

	}

	/**
	 * A bean report, contains a base information about a bean.
	 */
	protected static final class BeanReport {

		private final String name;

		private final BeanDefinition definition;

		public BeanReport(String name, BeanDefinition definition) {
			this.name = name;
			this.definition = definition;
		}

		/**
		 * Return the registered bean's name.
		 * @return the bean name.
		 */
		public String getName() {
			return this.name;
		}

		/**
		 * Return the registered {@code BeanDefinition} for the specified bean, or
		 * {@code null} if bean was programmatically registered.
		 * @return the registered {@code BeanDefinition}.
		 */

		public BeanDefinition getDefinition() {
			return this.definition;
		}

	}

}
