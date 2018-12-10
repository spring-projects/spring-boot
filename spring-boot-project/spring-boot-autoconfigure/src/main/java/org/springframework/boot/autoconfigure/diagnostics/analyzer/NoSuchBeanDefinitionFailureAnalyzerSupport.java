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

package org.springframework.boot.autoconfigure.diagnostics.analyzer;

import java.lang.annotation.Annotation;
import java.util.List;

import org.springframework.aop.framework.autoproxy.AutoProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.core.ResolvableType;
import org.springframework.core.type.MethodMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Base {@link AbstractNoSuchBeanDefinitionFailureAnalyzer} which provides a basic
 * implementation of the building {@code FailureAnalysis}.
 *
 * @param <T> the type of exception to analyze
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Dmytro Nosan
 */
public abstract class NoSuchBeanDefinitionFailureAnalyzerSupport<T extends Throwable>
		extends AbstractNoSuchBeanDefinitionFailureAnalyzer<T> {

	private ConfigurableListableBeanFactory beanFactory;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		Assert.isInstanceOf(ConfigurableListableBeanFactory.class, beanFactory);
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
		super.setBeanFactory(beanFactory);
	}

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, T cause,
			String injectionPointDescription, ConfigurationReport configurationReport,
			BeanMetadata beanMetadata) {

		String description = getDescription(rootFailure, cause, configurationReport,
				injectionPointDescription, beanMetadata);

		if (description == null) {
			return null;
		}

		String action = getAction(rootFailure, cause, configurationReport, beanMetadata);

		return new FailureAnalysis(description, action, cause);
	}

	/**
	 * Returns a description of the failure, or {@code null} if no analysis was possible.
	 * @param rootFailure the root failure passed to the analyzer
	 * @param cause the actual found cause
	 * @param configurationReport the configuration report.
	 * @param injectionPointDescription the description of the injection point or
	 * {@code null}
	 * @param beanMetadata the bean metadata.
	 * @return description of the failure.
	 */

	protected String getDescription(Throwable rootFailure, T cause,
			ConfigurationReport configurationReport, String injectionPointDescription,
			BeanMetadata beanMetadata) {
		List<AutoConfigurationReport> autoConfigurationReports = configurationReport
				.getAutoConfigurationReports();
		List<BeanReport> beanReports = configurationReport.getBeanReports();
		StringBuilder message = new StringBuilder();
		List<Annotation> injectionAnnotations = configurationReport
				.getInjectionAnnotations();
		if (!injectionAnnotations.isEmpty()) {
			message.append(String
					.format("%nThe injection point has the following annotations:%n"));
			for (Annotation injectionAnnotation : injectionAnnotations) {
				message.append(String.format("\t- %s%n", injectionAnnotation));
			}
		}
		message.append(
				String.format("%s required a %s that could not be found.%n",
						(injectionPointDescription != null) ? injectionPointDescription
								: "A component",
						getBeanMetadataDescription(beanMetadata)));
		for (AutoConfigurationReport autoConfigurationReport : autoConfigurationReports) {
			message.append(String.format("\t- %s%n",
					getAutoConfigurationReportDescription(autoConfigurationReport)));
		}
		for (BeanReport beanReport : beanReports) {
			message.append(
					String.format("\t- %s%n", getBeanReportDescription(beanReport)));
		}
		return message.toString();
	}

	/**
	 * Returns an action of the failure, or {@code null}.
	 * @param rootFailure the root failure passed to the analyzer
	 * @param cause the actual found cause
	 * @param configurationReport the configuration report.
	 * @param beanMetadata the bean metadata.
	 * @return action of the failure.
	 */

	protected String getAction(Throwable rootFailure, T cause,
			ConfigurationReport configurationReport, BeanMetadata beanMetadata) {

		List<AutoConfigurationReport> autoConfigurationReports = configurationReport
				.getAutoConfigurationReports();
		List<BeanReport> beanReports = configurationReport.getBeanReports();
		return String.format("Consider %s a %s in your configuration.",
				(!autoConfigurationReports.isEmpty() || !beanReports.isEmpty())
						? "revisiting the entries above or defining" : "defining",
				getBeanMetadataDescription(beanMetadata));

	}

	/**
	 * Return the description of the {@code BeanReport}.
	 * @param beanReport the report of the bean definition.
	 * @return description of the {@code BeanReport}.
	 */
	protected String getBeanReportDescription(BeanReport beanReport) {
		BeanDefinition definition = beanReport.getDefinition();
		StringBuilder description = new StringBuilder(
				String.format("User-defined bean '%s'", beanReport.getName()));

		Class<?> beanClass = AutoProxyUtils.determineTargetClass(this.beanFactory,
				beanReport.getName());
		if (beanClass != null) {
			description.append(String.format(" of type '%s'", beanClass.getName()));
		}

		if (StringUtils.hasText(definition.getFactoryMethodName())) {
			description.append(String.format(" : defined by method '%s'",
					definition.getFactoryMethodName()));
		}

		MethodMetadata methodMetadata = getFactoryMethodMetadata(definition);
		if (methodMetadata != null
				&& StringUtils.hasText(methodMetadata.getDeclaringClassName())) {
			description.append(String.format(" in %s",
					ClassUtils.getShortName(methodMetadata.getDeclaringClassName())));
		}
		else if (StringUtils.hasText(definition.getResourceDescription())) {
			description
					.append(String.format(" in %s", definition.getResourceDescription()));
		}

		if (beanReport.isNullBean()) {
			description.append(" could be ignored as the bean value is null");
		}
		return description.toString();
	}

	/**
	 * Return the description of the {@code AutoConfigurationReport}.
	 * @param autoConfigurationReport the report of the auto-configuration.
	 * @return description of the {@code AutoConfigurationReport}.
	 */
	protected String getAutoConfigurationReportDescription(
			AutoConfigurationReport autoConfigurationReport) {
		MethodMetadata methodMetadata = autoConfigurationReport.getMethodMetadata();
		ConditionOutcome conditionOutcome = autoConfigurationReport.getConditionOutcome();
		return String.format("Bean method '%s' in '%s' not loaded because %s",
				methodMetadata.getMethodName(),
				ClassUtils.getShortName(methodMetadata.getDeclaringClassName()),
				conditionOutcome.getMessage());
	}

	/**
	 * Return the description of the {@code BeanMetadata}.
	 * @param beanMetadata the metadata of the bean.
	 * @return description of the {@code BeanMetadata}.
	 */
	protected String getBeanMetadataDescription(BeanMetadata beanMetadata) {
		ResolvableType resolvableType = beanMetadata.getResolvableType();
		Class<?> type = (resolvableType != null) ? resolvableType.getRawClass() : null;
		if (beanMetadata.getName() != null && type != null) {
			return "bean named '" + beanMetadata.getName() + "' of type '"
					+ type.getName() + "'";
		}
		if (type != null) {
			return "bean of type '" + type.getName() + "'";
		}
		return "bean named '" + beanMetadata.getName() + "'";
	}

	private MethodMetadata getFactoryMethodMetadata(BeanDefinition bd) {
		if (bd instanceof AnnotatedBeanDefinition) {
			return ((AnnotatedBeanDefinition) bd).getFactoryMethodMetadata();
		}
		return null;
	}

}
