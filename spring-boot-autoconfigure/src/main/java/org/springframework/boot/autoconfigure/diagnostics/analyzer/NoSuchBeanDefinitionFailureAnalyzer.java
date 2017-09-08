/*
 * Copyright 2012-2017 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport.ConditionAndOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport.ConditionAndOutcomes;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.analyzer.AbstractInjectionFailureAnalyzer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ResolvableType;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * An {@link AbstractInjectionFailureAnalyzer} that performs analysis of failures caused
 * by a {@link NoSuchBeanDefinitionException}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class NoSuchBeanDefinitionFailureAnalyzer
		extends AbstractInjectionFailureAnalyzer<NoSuchBeanDefinitionException>
		implements BeanFactoryAware {

	private ConfigurableListableBeanFactory beanFactory;

	private MetadataReaderFactory metadataReaderFactory;

	private ConditionEvaluationReport report;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		Assert.isInstanceOf(ConfigurableListableBeanFactory.class, beanFactory);
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
		this.metadataReaderFactory = new CachingMetadataReaderFactory(
				this.beanFactory.getBeanClassLoader());
		// Get early as won't be accessible once context has failed to start
		this.report = ConditionEvaluationReport.get(this.beanFactory);
	}

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure,
			NoSuchBeanDefinitionException cause, String description) {
		if (cause.getNumberOfBeansFound() != 0) {
			return null;
		}
		List<AutoConfigurationResult> autoConfigurationResults = getAutoConfigurationResults(
				cause);
		StringBuilder message = new StringBuilder();
		message.append(String.format("%s required %s that could not be found.%n",
				description == null ? "A component" : description,
				getBeanDescription(cause)));
		if (!autoConfigurationResults.isEmpty()) {
			for (AutoConfigurationResult provider : autoConfigurationResults) {
				message.append(String.format("\t- %s%n", provider));
			}
		}
		String action = String.format("Consider %s %s in your configuration.",
				(!autoConfigurationResults.isEmpty()
						? "revisiting the conditions above or defining" : "defining"),
				getBeanDescription(cause));
		return new FailureAnalysis(message.toString(), action, cause);
	}

	private String getBeanDescription(NoSuchBeanDefinitionException cause) {
		if (cause.getResolvableType() != null) {
			Class<?> type = extractBeanType(cause.getResolvableType());
			return "a bean of type '" + type.getName() + "'";
		}
		return "a bean named '" + cause.getBeanName() + "'";
	}

	private Class<?> extractBeanType(ResolvableType resolvableType) {
		ResolvableType collectionType = resolvableType.asCollection();
		if (!collectionType.equals(ResolvableType.NONE)) {
			return collectionType.getGeneric(0).getRawClass();
		}
		ResolvableType mapType = resolvableType.asMap();
		if (!mapType.equals(ResolvableType.NONE)) {
			return mapType.getGeneric(1).getRawClass();
		}
		return resolvableType.getRawClass();
	}

	private List<AutoConfigurationResult> getAutoConfigurationResults(
			NoSuchBeanDefinitionException cause) {
		List<AutoConfigurationResult> results = new ArrayList<>();
		collectReportedConditionOutcomes(cause, results);
		collectExcludedAutoConfiguration(cause, results);
		return results;
	}

	private void collectReportedConditionOutcomes(NoSuchBeanDefinitionException cause,
			List<AutoConfigurationResult> results) {
		for (Map.Entry<String, ConditionAndOutcomes> entry : this.report
				.getConditionAndOutcomesBySource().entrySet()) {
			Source source = new Source(entry.getKey());
			ConditionAndOutcomes conditionAndOutcomes = entry.getValue();
			if (!conditionAndOutcomes.isFullMatch()) {
				BeanMethods methods = new BeanMethods(source, cause);
				for (ConditionAndOutcome conditionAndOutcome : conditionAndOutcomes) {
					if (!conditionAndOutcome.getOutcome().isMatch()) {
						for (MethodMetadata method : methods) {
							results.add(new AutoConfigurationResult(method,
									conditionAndOutcome.getOutcome(), source.isMethod()));
						}
					}
				}
			}
		}
	}

	private void collectExcludedAutoConfiguration(NoSuchBeanDefinitionException cause,
			List<AutoConfigurationResult> results) {
		for (String excludedClass : this.report.getExclusions()) {
			Source source = new Source(excludedClass);
			BeanMethods methods = new BeanMethods(source, cause);
			for (MethodMetadata method : methods) {
				String message = String.format("auto-configuration '%s' was excluded",
						ClassUtils.getShortName(excludedClass));
				results.add(new AutoConfigurationResult(method,
						new ConditionOutcome(false, message), false));
			}
		}
	}

	private class Source {

		private final String className;

		private final String methodName;

		Source(String source) {
			String[] tokens = source.split("#");
			this.className = (tokens.length > 1 ? tokens[0] : source);
			this.methodName = (tokens.length == 2 ? tokens[1] : null);
		}

		public String getClassName() {
			return this.className;
		}

		public String getMethodName() {
			return this.methodName;
		}

		public boolean isMethod() {
			return this.methodName != null;
		}

	}

	private class BeanMethods implements Iterable<MethodMetadata> {

		private final List<MethodMetadata> methods;

		BeanMethods(Source source, NoSuchBeanDefinitionException cause) {
			this.methods = findBeanMethods(source, cause);
		}

		private List<MethodMetadata> findBeanMethods(Source source,
				NoSuchBeanDefinitionException cause) {
			try {
				MetadataReader classMetadata = NoSuchBeanDefinitionFailureAnalyzer.this.metadataReaderFactory
						.getMetadataReader(source.getClassName());
				Set<MethodMetadata> candidates = classMetadata.getAnnotationMetadata()
						.getAnnotatedMethods(Bean.class.getName());
				List<MethodMetadata> result = new ArrayList<>();
				for (MethodMetadata candidate : candidates) {
					if (isMatch(candidate, source, cause)) {
						result.add(candidate);
					}
				}
				return Collections.unmodifiableList(result);
			}
			catch (Exception ex) {
				return Collections.emptyList();
			}
		}

		private boolean isMatch(MethodMetadata candidate, Source source,
				NoSuchBeanDefinitionException cause) {
			if (source.getMethodName() != null
					&& !source.getMethodName().equals(candidate.getMethodName())) {
				return false;
			}
			String name = cause.getBeanName();
			ResolvableType resolvableType = cause.getResolvableType();
			return ((name != null && hasName(candidate, name)) || (resolvableType != null
					&& hasType(candidate, extractBeanType(resolvableType))));
		}

		private boolean hasName(MethodMetadata methodMetadata, String name) {
			Map<String, Object> attributes = methodMetadata
					.getAnnotationAttributes(Bean.class.getName());
			String[] candidates = (attributes == null ? null
					: (String[]) attributes.get("name"));
			if (candidates != null) {
				for (String candidate : candidates) {
					if (candidate.equals(name)) {
						return true;
					}
				}
				return false;
			}
			return methodMetadata.getMethodName().equals(name);
		}

		private boolean hasType(MethodMetadata candidate, Class<?> type) {
			String returnTypeName = candidate.getReturnTypeName();
			if (type.getName().equals(returnTypeName)) {
				return true;
			}
			try {
				Class<?> returnType = ClassUtils.forName(returnTypeName,
						NoSuchBeanDefinitionFailureAnalyzer.this.beanFactory
								.getBeanClassLoader());
				return type.isAssignableFrom(returnType);
			}
			catch (Throwable ex) {
				return false;
			}
		}

		@Override
		public Iterator<MethodMetadata> iterator() {
			return this.methods.iterator();
		}

	}

	private class AutoConfigurationResult {

		private final MethodMetadata methodMetadata;

		private final ConditionOutcome conditionOutcome;

		private final boolean methodEvaluated;

		AutoConfigurationResult(MethodMetadata methodMetadata,
				ConditionOutcome conditionOutcome, boolean methodEvaluated) {
			this.methodMetadata = methodMetadata;
			this.conditionOutcome = conditionOutcome;
			this.methodEvaluated = methodEvaluated;
		}

		@Override
		public String toString() {
			if (this.methodEvaluated) {
				return String.format("Bean method '%s' in '%s' not loaded because %s",
						this.methodMetadata.getMethodName(),
						ClassUtils.getShortName(
								this.methodMetadata.getDeclaringClassName()),
						this.conditionOutcome.getMessage());
			}
			return String.format("Bean method '%s' not loaded because %s",
					this.methodMetadata.getMethodName(),
					this.conditionOutcome.getMessage());
		}

	}

}
