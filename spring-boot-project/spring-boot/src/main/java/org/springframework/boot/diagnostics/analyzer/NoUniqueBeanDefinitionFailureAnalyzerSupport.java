/*
 * Copyright 2012-2019 the original author or authors.
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

import java.util.List;

import org.springframework.aop.framework.autoproxy.AutoProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Base {@link AbstractNoUniqueBeanDefinitionFailureAnalyzer} which provides a basic
 * implementation of the building {@code FailureAnalysis}.
 *
 * @param <T> the type of exception to analyze
 * @author Andy Wilkinson
 * @author Dmytro Nosan
 */
public abstract class NoUniqueBeanDefinitionFailureAnalyzerSupport<T extends Throwable>
		extends AbstractNoUniqueBeanDefinitionFailureAnalyzer<T> {

	private ConfigurableListableBeanFactory beanFactory;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		Assert.isInstanceOf(ConfigurableListableBeanFactory.class, beanFactory);
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
		super.setBeanFactory(beanFactory);
	}

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, T cause,
			String injectionPointDescription, ConfigurationReport configurationReport) {

		String description = getDescription(rootFailure, cause, injectionPointDescription,
				configurationReport);

		if (description == null) {
			return null;
		}

		String action = getAction(rootFailure, cause, configurationReport);

		return new FailureAnalysis(description, action, cause);
	}

	/**
	 * Returns a description of the failure, or {@code null} if no analysis was possible.
	 * @param rootFailure the root failure passed to the analyzer
	 * @param cause the actual found cause
	 * @param injectionPointDescription the description of the injection point or
	 * {@code null}
	 * @param configurationReport the configuration report.
	 * @return description of the failure.
	 */

	protected String getDescription(Throwable rootFailure, T cause,
			String injectionPointDescription, ConfigurationReport configurationReport) {
		StringBuilder description = new StringBuilder();
		List<BeanReport> beanReports = configurationReport.getBeanReports();
		description
				.append(String
						.format("%s required a single bean, but %d were found:%n",
								(injectionPointDescription != null)
										? injectionPointDescription : "A component",
								beanReports.size()));
		for (BeanReport beanReport : beanReports) {
			description.append(
					String.format("\t- %s%n", getBeanReportDescription(beanReport)));
		}

		return description.toString();
	}

	/**
	 * Returns an action of the failure, or {@code null}.
	 * @param rootFailure the root failure passed to the analyzer
	 * @param cause the actual found cause
	 * @param configurationReport the configuration report.
	 * @return action of the failure.
	 */

	protected String getAction(Throwable rootFailure, T cause,
			ConfigurationReport configurationReport) {
		return "Consider marking one of the beans as @Primary, updating the consumer to"
				+ " accept multiple beans, or using @Qualifier to identify the"
				+ " bean that should be consumed";

	}

	/**
	 * Return the description of the {@code BeanReport}.
	 * @param beanReport the report of the bean definition.
	 * @return description of the {@code BeanReport}.
	 */
	protected String getBeanReportDescription(BeanReport beanReport) {
		StringBuilder description = new StringBuilder(
				String.format("'%s'", beanReport.getName()));
		BeanDefinition definition = beanReport.getDefinition();
		if (definition != null) {
			Class<?> beanClass = AutoProxyUtils.determineTargetClass(this.beanFactory,
					beanReport.getName());
			if (beanClass != null) {
				description.append(String.format(" of type '%s'", beanClass.getName()));
			}
			if (StringUtils.hasText(definition.getFactoryMethodName())) {
				description.append(String.format(" defined by method '%s'",
						definition.getFactoryMethodName()));
			}
			if (StringUtils.hasText(definition.getResourceDescription())) {
				description.append(
						String.format(" in %s", definition.getResourceDescription()));
			}
		}
		else {
			description.append(": a programmatically registered singleton");
		}

		return description.toString();
	}

}
