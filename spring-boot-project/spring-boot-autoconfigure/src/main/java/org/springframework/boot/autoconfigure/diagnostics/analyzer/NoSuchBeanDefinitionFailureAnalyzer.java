/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.diagnostics.analyzer;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
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
 * @author Scott Frederick
 */
class NoSuchBeanDefinitionFailureAnalyzer extends AbstractInjectionFailureAnalyzer<NoSuchBeanDefinitionException> {

	private final ConfigurableListableBeanFactory beanFactory;

	private final MetadataReaderFactory metadataReaderFactory;

	private final ConditionEvaluationReport report;

	/**
	 * Constructs a new instance of NoSuchBeanDefinitionFailureAnalyzer with the specified
	 * bean factory.
	 * @param beanFactory the bean factory to be used by the failure analyzer
	 * @throws IllegalArgumentException if the bean factory is not an instance of
	 * ConfigurableListableBeanFactory
	 */
	NoSuchBeanDefinitionFailureAnalyzer(BeanFactory beanFactory) {
		Assert.isInstanceOf(ConfigurableListableBeanFactory.class, beanFactory);
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
		this.metadataReaderFactory = new CachingMetadataReaderFactory(this.beanFactory.getBeanClassLoader());
		// Get early as won't be accessible once context has failed to start
		this.report = ConditionEvaluationReport.get(this.beanFactory);
	}

	/**
	 * Analyzes the failure caused by a NoSuchBeanDefinitionException.
	 * @param rootFailure the root cause of the failure
	 * @param cause the NoSuchBeanDefinitionException that caused the failure
	 * @param description the description of the component that required the bean
	 * @return a FailureAnalysis object containing the analysis result
	 */
	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, NoSuchBeanDefinitionException cause, String description) {
		if (cause.getNumberOfBeansFound() != 0) {
			return null;
		}
		List<AutoConfigurationResult> autoConfigurationResults = getAutoConfigurationResults(cause);
		List<UserConfigurationResult> userConfigurationResults = getUserConfigurationResults(cause);
		StringBuilder message = new StringBuilder();
		message.append(String.format("%s required %s that could not be found.%n",
				(description != null) ? description : "A component", getBeanDescription(cause)));
		InjectionPoint injectionPoint = findInjectionPoint(rootFailure);
		if (injectionPoint != null) {
			Annotation[] injectionAnnotations = injectionPoint.getAnnotations();
			if (injectionAnnotations.length > 0) {
				message.append(String.format("%nThe injection point has the following annotations:%n"));
				for (Annotation injectionAnnotation : injectionAnnotations) {
					message.append(String.format("\t- %s%n", injectionAnnotation));
				}
			}
		}
		if (!autoConfigurationResults.isEmpty() || !userConfigurationResults.isEmpty()) {
			message.append(String.format("%nThe following candidates were found but could not be injected:%n"));
			for (AutoConfigurationResult result : autoConfigurationResults) {
				message.append(String.format("\t- %s%n", result));
			}
			for (UserConfigurationResult result : userConfigurationResults) {
				message.append(String.format("\t- %s%n", result));
			}
		}
		String action = String.format("Consider %s %s in your configuration.",
				(!autoConfigurationResults.isEmpty() || !userConfigurationResults.isEmpty())
						? "revisiting the entries above or defining" : "defining",
				getBeanDescription(cause));
		return new FailureAnalysis(message.toString(), action, cause);
	}

	/**
	 * Returns the description of the bean that caused the NoSuchBeanDefinitionException.
	 * @param cause the NoSuchBeanDefinitionException that occurred
	 * @return the description of the bean
	 */
	private String getBeanDescription(NoSuchBeanDefinitionException cause) {
		if (cause.getResolvableType() != null) {
			Class<?> type = extractBeanType(cause.getResolvableType());
			return "a bean of type '" + type.getName() + "'";
		}
		return "a bean named '" + cause.getBeanName() + "'";
	}

	/**
	 * Extracts the bean type from the given ResolvableType.
	 * @param resolvableType the ResolvableType to extract the bean type from
	 * @return the Class representing the bean type
	 */
	private Class<?> extractBeanType(ResolvableType resolvableType) {
		return resolvableType.getRawClass();
	}

	/**
	 * Retrieves the auto configuration results for a
	 * {@link NoSuchBeanDefinitionException}.
	 * @param cause the {@link NoSuchBeanDefinitionException} that occurred
	 * @return a list of {@link AutoConfigurationResult} objects representing the auto
	 * configuration results
	 */
	private List<AutoConfigurationResult> getAutoConfigurationResults(NoSuchBeanDefinitionException cause) {
		List<AutoConfigurationResult> results = new ArrayList<>();
		collectReportedConditionOutcomes(cause, results);
		collectExcludedAutoConfiguration(cause, results);
		return results;
	}

	/**
	 * Retrieves a list of UserConfigurationResult objects based on the given
	 * NoSuchBeanDefinitionException cause.
	 * @param cause The NoSuchBeanDefinitionException cause.
	 * @return A list of UserConfigurationResult objects.
	 */
	private List<UserConfigurationResult> getUserConfigurationResults(NoSuchBeanDefinitionException cause) {
		ResolvableType type = cause.getResolvableType();
		if (type == null) {
			return Collections.emptyList();
		}
		String[] beanNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(this.beanFactory, type);
		return Arrays.stream(beanNames)
			.map((beanName) -> new UserConfigurationResult(getFactoryMethodMetadata(beanName),
					this.beanFactory.getBean(beanName).equals(null)))
			.toList();
	}

	/**
	 * Retrieves the metadata of the factory method for the specified bean.
	 * @param beanName the name of the bean
	 * @return the metadata of the factory method, or null if not found
	 */
	private MethodMetadata getFactoryMethodMetadata(String beanName) {
		BeanDefinition beanDefinition = this.beanFactory.getBeanDefinition(beanName);
		if (beanDefinition instanceof AnnotatedBeanDefinition annotatedBeanDefinition) {
			return annotatedBeanDefinition.getFactoryMethodMetadata();
		}
		return null;
	}

	/**
	 * Collects the reported condition outcomes for a NoSuchBeanDefinitionException.
	 * @param cause the NoSuchBeanDefinitionException that occurred
	 * @param results the list of AutoConfigurationResults to collect the outcomes into
	 */
	private void collectReportedConditionOutcomes(NoSuchBeanDefinitionException cause,
			List<AutoConfigurationResult> results) {
		this.report.getConditionAndOutcomesBySource()
			.forEach((source, sourceOutcomes) -> collectReportedConditionOutcomes(cause, new Source(source),
					sourceOutcomes, results));
	}

	/**
	 * Collects the reported condition outcomes for a NoSuchBeanDefinitionException.
	 * @param cause The NoSuchBeanDefinitionException that occurred.
	 * @param source The source of the exception.
	 * @param sourceOutcomes The condition and outcomes associated with the source.
	 * @param results The list of auto configuration results.
	 */
	private void collectReportedConditionOutcomes(NoSuchBeanDefinitionException cause, Source source,
			ConditionAndOutcomes sourceOutcomes, List<AutoConfigurationResult> results) {
		if (sourceOutcomes.isFullMatch()) {
			return;
		}
		BeanMethods methods = new BeanMethods(source, cause);
		for (ConditionAndOutcome conditionAndOutcome : sourceOutcomes) {
			if (!conditionAndOutcome.getOutcome().isMatch()) {
				for (MethodMetadata method : methods) {
					results.add(new AutoConfigurationResult(method, conditionAndOutcome.getOutcome()));
				}
			}
		}
	}

	/**
	 * Collects the excluded auto-configuration classes and adds them to the list of
	 * results.
	 * @param cause the NoSuchBeanDefinitionException that triggered the failure analysis
	 * @param results the list of AutoConfigurationResult objects to add the excluded
	 * auto-configuration classes to
	 */
	private void collectExcludedAutoConfiguration(NoSuchBeanDefinitionException cause,
			List<AutoConfigurationResult> results) {
		for (String excludedClass : this.report.getExclusions()) {
			Source source = new Source(excludedClass);
			BeanMethods methods = new BeanMethods(source, cause);
			for (MethodMetadata method : methods) {
				String message = String.format("auto-configuration '%s' was excluded",
						ClassUtils.getShortName(excludedClass));
				results.add(new AutoConfigurationResult(method, new ConditionOutcome(false, message)));
			}
		}
	}

	/**
	 * Finds the injection point that caused the given failure.
	 * @param failure the Throwable object representing the failure
	 * @return the InjectionPoint object representing the injection point that caused the
	 * failure, or null if no injection point was found
	 */
	private InjectionPoint findInjectionPoint(Throwable failure) {
		UnsatisfiedDependencyException unsatisfiedDependencyException = findCause(failure,
				UnsatisfiedDependencyException.class);
		if (unsatisfiedDependencyException == null) {
			return null;
		}
		return unsatisfiedDependencyException.getInjectionPoint();
	}

	/**
	 * Source class.
	 */
	private static class Source {

		private final String className;

		private final String methodName;

		/**
		 * Constructs a new Source object with the given source string.
		 *
		 * The source string is split using the "#" delimiter. If the source string
		 * contains more than one "#" delimiter, the first token is assigned to the
		 * className field, and the second token is assigned to the methodName field. If
		 * the source string does not contain any "#" delimiter, the entire source string
		 * is assigned to the className field, and the methodName field is set to null.
		 * @param source the source string to be used for constructing the Source object
		 */
		Source(String source) {
			String[] tokens = source.split("#");
			this.className = (tokens.length > 1) ? tokens[0] : source;
			this.methodName = (tokens.length != 2) ? null : tokens[1];
		}

		/**
		 * Returns the name of the class.
		 * @return the name of the class
		 */
		String getClassName() {
			return this.className;
		}

		/**
		 * Returns the name of the method.
		 * @return the name of the method
		 */
		String getMethodName() {
			return this.methodName;
		}

	}

	/**
	 * BeanMethods class.
	 */
	private class BeanMethods implements Iterable<MethodMetadata> {

		private final List<MethodMetadata> methods;

		/**
		 * Constructs a new BeanMethods object with the specified source and cause.
		 * @param source the source object used to find bean methods
		 * @param cause the exception that caused the bean definition to be missing
		 */
		BeanMethods(Source source, NoSuchBeanDefinitionException cause) {
			this.methods = findBeanMethods(source, cause);
		}

		/**
		 * Finds the bean methods in the given source with the specified
		 * NoSuchBeanDefinitionException cause.
		 * @param source The source object containing the class name.
		 * @param cause The NoSuchBeanDefinitionException cause.
		 * @return A list of MethodMetadata objects representing the bean methods found in
		 * the source.
		 * @throws Exception If an error occurs during the process.
		 */
		private List<MethodMetadata> findBeanMethods(Source source, NoSuchBeanDefinitionException cause) {
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

		/**
		 * Checks if the given candidate method metadata matches the source and cause of a
		 * NoSuchBeanDefinitionException.
		 * @param candidate the method metadata to check
		 * @param source the source of the NoSuchBeanDefinitionException
		 * @param cause the NoSuchBeanDefinitionException
		 * @return true if the candidate method metadata matches the source and cause,
		 * false otherwise
		 */
		private boolean isMatch(MethodMetadata candidate, Source source, NoSuchBeanDefinitionException cause) {
			if (source.getMethodName() != null && !source.getMethodName().equals(candidate.getMethodName())) {
				return false;
			}
			String name = cause.getBeanName();
			ResolvableType resolvableType = cause.getResolvableType();
			return ((name != null && hasName(candidate, name))
					|| (resolvableType != null && hasType(candidate, extractBeanType(resolvableType))));
		}

		/**
		 * Checks if the given method has a specific name.
		 * @param methodMetadata the metadata of the method
		 * @param name the name to check against
		 * @return true if the method has the specified name, false otherwise
		 */
		private boolean hasName(MethodMetadata methodMetadata, String name) {
			Map<String, Object> attributes = methodMetadata.getAnnotationAttributes(Bean.class.getName());
			String[] candidates = (attributes != null) ? (String[]) attributes.get("name") : null;
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

		/**
		 * Checks if the given MethodMetadata object has a return type that matches the
		 * specified type.
		 * @param candidate The MethodMetadata object to check.
		 * @param type The type to compare the return type against.
		 * @return true if the return type matches the specified type, false otherwise.
		 */
		private boolean hasType(MethodMetadata candidate, Class<?> type) {
			String returnTypeName = candidate.getReturnTypeName();
			if (type.getName().equals(returnTypeName)) {
				return true;
			}
			try {
				Class<?> returnType = ClassUtils.forName(returnTypeName,
						NoSuchBeanDefinitionFailureAnalyzer.this.beanFactory.getBeanClassLoader());
				return type.isAssignableFrom(returnType);
			}
			catch (Throwable ex) {
				return false;
			}
		}

		/**
		 * Returns an iterator over the methods in this BeanMethods object.
		 * @return an iterator over the methods in this BeanMethods object
		 */
		@Override
		public Iterator<MethodMetadata> iterator() {
			return this.methods.iterator();
		}

	}

	/**
	 * AutoConfigurationResult class.
	 */
	private static class AutoConfigurationResult {

		private final MethodMetadata methodMetadata;

		private final ConditionOutcome conditionOutcome;

		/**
		 * Constructs a new AutoConfigurationResult with the given MethodMetadata and
		 * ConditionOutcome.
		 * @param methodMetadata the MethodMetadata associated with this
		 * AutoConfigurationResult
		 * @param conditionOutcome the ConditionOutcome associated with this
		 * AutoConfigurationResult
		 */
		AutoConfigurationResult(MethodMetadata methodMetadata, ConditionOutcome conditionOutcome) {
			this.methodMetadata = methodMetadata;
			this.conditionOutcome = conditionOutcome;
		}

		/**
		 * Returns a string representation of this AutoConfigurationResult object.
		 * @return a formatted string indicating the bean method that was not loaded and
		 * the reason why it was not loaded
		 */
		@Override
		public String toString() {
			return String.format("Bean method '%s' in '%s' not loaded because %s", this.methodMetadata.getMethodName(),
					ClassUtils.getShortName(this.methodMetadata.getDeclaringClassName()),
					this.conditionOutcome.getMessage());
		}

	}

	/**
	 * UserConfigurationResult class.
	 */
	private static class UserConfigurationResult {

		private final MethodMetadata methodMetadata;

		private final boolean nullBean;

		/**
		 * Constructs a new UserConfigurationResult with the specified method metadata and
		 * null bean flag.
		 * @param methodMetadata the method metadata associated with the result
		 * @param nullBean true if the bean is null, false otherwise
		 */
		UserConfigurationResult(MethodMetadata methodMetadata, boolean nullBean) {
			this.methodMetadata = methodMetadata;
			this.nullBean = nullBean;
		}

		/**
		 * Returns a string representation of the UserConfigurationResult object.
		 * @return a string representation of the UserConfigurationResult object
		 */
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("User-defined bean");
			if (this.methodMetadata != null) {
				sb.append(String.format(" method '%s' in '%s'", this.methodMetadata.getMethodName(),
						ClassUtils.getShortName(this.methodMetadata.getDeclaringClassName())));
			}
			if (this.nullBean) {
				sb.append(" ignored as the bean value is null");
			}
			return sb.toString();
		}

	}

}
