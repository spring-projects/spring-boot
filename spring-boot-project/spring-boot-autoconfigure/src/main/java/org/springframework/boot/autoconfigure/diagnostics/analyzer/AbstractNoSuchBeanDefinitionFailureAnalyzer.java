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

package org.springframework.boot.autoconfigure.diagnostics.analyzer;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
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
 * Abstract base class for a {@link AbstractInjectionFailureAnalyzer} that inspects the
 * {@code not matches} auto-configurations and registered beans for provided
 * {@code BeanMetadata}.
 *
 * @param <T> the type of exception to analyze
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Dmytro Nosan
 */
public abstract class AbstractNoSuchBeanDefinitionFailureAnalyzer<T extends Throwable>
		extends AbstractInjectionFailureAnalyzer<T> implements BeanFactoryAware {

	private ConfigurableListableBeanFactory beanFactory;

	private ClassLoader beanClassLoader;

	private MetadataReaderFactory metadataReaderFactory;

	private ConditionEvaluationReport report;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		Assert.isInstanceOf(ConfigurableListableBeanFactory.class, beanFactory);
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
		this.beanClassLoader = this.beanFactory.getBeanClassLoader();
		this.metadataReaderFactory = new CachingMetadataReaderFactory(
				this.beanClassLoader);
		// Get early as won't be accessible once context has failed to start
		this.report = ConditionEvaluationReport.get(this.beanFactory);
	}

	@Override
	protected final FailureAnalysis analyze(Throwable rootFailure, T cause,
			String injectionPointDescription) {

		BeanMetadata beanMetadata = getBeanMetadata(rootFailure, cause);

		if (beanMetadata == null) {
			return null;
		}

		List<AutoConfigurationReport> autoConfigurationReports = getAutoConfigurationReports(
				beanMetadata);
		List<BeanReport> beanReports = getBeanReports(beanMetadata);

		List<Annotation> injectionAnnotations = findInjectionAnnotations(rootFailure);

		return analyze(rootFailure, cause, injectionPointDescription,
				new ConfigurationReport(autoConfigurationReports, beanReports,
						injectionAnnotations),
				beanMetadata);
	}

	/**
	 * Returns an analysis of the given {@code rootFailure}, or {@code null} if no
	 * analysis was possible.
	 * @param rootFailure the root failure passed to the analyzer
	 * @param cause the actual found cause
	 * @param configurationReport the configuration report.
	 * @param injectionPointDescription the description of the injection point or
	 * {@code null}
	 * @param beanMetadata the bean metadata.
	 * @return the analysis or {@code null}
	 */

	protected abstract FailureAnalysis analyze(Throwable rootFailure, T cause,
			String injectionPointDescription, ConfigurationReport configurationReport,
			BeanMetadata beanMetadata);

	/**
	 * Returns a bean metadata of the given {@code rootFailure} and {@code cause}, or
	 * {@code null} if no analysis was possible.
	 * @param rootFailure the root failure passed to the analyzer
	 * @param cause the actual found cause
	 * @return the bean metadata or {@code null}
	 **/

	protected abstract BeanMetadata getBeanMetadata(Throwable rootFailure, T cause);

	private List<AutoConfigurationReport> getAutoConfigurationReports(
			BeanMetadata beanMetadata) {
		List<AutoConfigurationReport> result = new ArrayList<>();
		result.addAll(getReportedConditionOutcomes(beanMetadata));
		result.addAll(getExcludedAutoConfigurations(beanMetadata));
		return result;
	}

	private List<Annotation> findInjectionAnnotations(Throwable failure) {
		UnsatisfiedDependencyException unsatisfiedDependencyException = findCause(failure,
				UnsatisfiedDependencyException.class);
		if (unsatisfiedDependencyException == null) {
			return Collections.emptyList();
		}
		return Arrays.asList(
				unsatisfiedDependencyException.getInjectionPoint().getAnnotations());
	}

	private List<AutoConfigurationReport> getReportedConditionOutcomes(
			BeanMetadata beanMetadata) {
		List<AutoConfigurationReport> result = new ArrayList<>();

		this.report.getConditionAndOutcomesBySource()
				.forEach((s, conditionAndOutcomes) -> {
					Source source = new Source(s);
					if (conditionAndOutcomes.isFullMatch()) {
						return;
					}
					BeanMethods methods = new BeanMethods(source, beanMetadata,
							this.metadataReaderFactory, this.beanClassLoader);
					for (ConditionEvaluationReport.ConditionAndOutcome conditionAndOutcome : conditionAndOutcomes) {
						for (MethodMetadata methodMetadata : methods) {
							if (!conditionAndOutcome.getOutcome().isMatch()) {
								result.add(new AutoConfigurationReport(methodMetadata,
										conditionAndOutcome.getOutcome()));
							}
						}
					}
				});
		return result;
	}

	private List<AutoConfigurationReport> getExcludedAutoConfigurations(
			BeanMetadata beanMetadata) {
		List<AutoConfigurationReport> result = new ArrayList<>();
		for (String excludedClass : this.report.getExclusions()) {
			Source source = new Source(excludedClass);
			BeanMethods methods = new BeanMethods(source, beanMetadata,
					this.metadataReaderFactory, this.beanClassLoader);
			for (MethodMetadata methodMetadata : methods) {
				String message = String.format("auto-configuration '%s' was excluded",
						ClassUtils.getShortName(excludedClass));
				result.add(new AutoConfigurationReport(methodMetadata,
						new ConditionOutcome(false, message)));
			}
		}
		return result;
	}

	private List<BeanReport> getBeanReports(BeanMetadata beanMetadata) {
		ResolvableType resolvableType = beanMetadata.getResolvableType();
		if (resolvableType == null) {
			return Collections.emptyList();
		}
		String[] names = BeanFactoryUtils
				.beanNamesForTypeIncludingAncestors(this.beanFactory, resolvableType);
		List<BeanReport> beanReports = new ArrayList<>();
		for (String name : names) {
			try {
				BeanDefinition bd = this.beanFactory.getMergedBeanDefinition(name);
				Object bean = this.beanFactory.getBean(name);
				beanReports.add(new BeanReport(name, isNullBean(bean), bd));
			}
			catch (NoSuchBeanDefinitionException ignore) {
			}
		}
		return beanReports;
	}

	private static boolean isNullBean(Object bean) {
		// see org.springframework.beans.factory.support.NullBean
		return bean.equals(null);
	}

	private static Class<?> forName(String name, ClassLoader classLoader) {
		try {
			return ClassUtils.forName(name, classLoader);
		}
		catch (Throwable ex) {
			return null;
		}
	}

	/**
	 * A configuration report.
	 */
	protected static final class ConfigurationReport {

		private final List<AutoConfigurationReport> autoConfigurationReports;

		private final List<BeanReport> beanReports;

		private final List<Annotation> injectionAnnotations;

		public ConfigurationReport(List<AutoConfigurationReport> autoConfigurationReports,
				List<BeanReport> beanReports, List<Annotation> injectionAnnotations) {
			this.autoConfigurationReports = Collections
					.unmodifiableList(autoConfigurationReports);
			this.beanReports = Collections.unmodifiableList(beanReports);
			this.injectionAnnotations = injectionAnnotations;
		}

		/**
		 * Return the auto-configuration reports.
		 * @return the list of {@link AutoConfigurationReport}
		 */
		public List<AutoConfigurationReport> getAutoConfigurationReports() {
			return this.autoConfigurationReports;
		}

		/**
		 * Return the bean reports.
		 * @return the list of {@link BeanReport}
		 */
		public List<BeanReport> getBeanReports() {
			return this.beanReports;
		}

		/**
		 * Return the injection point annotations.
		 * @return the injection annotations.
		 */
		public List<Annotation> getInjectionAnnotations() {
			return this.injectionAnnotations;
		}

	}

	/**
	 * An auto-configuration report. Contains only {@code not matches} reports.
	 */
	protected static final class AutoConfigurationReport {

		private final MethodMetadata methodMetadata;

		private final ConditionOutcome conditionOutcome;

		public AutoConfigurationReport(MethodMetadata methodMetadata,
				ConditionOutcome conditionOutcome) {
			this.methodMetadata = methodMetadata;
			this.conditionOutcome = conditionOutcome;
		}

		/**
		 * Return the {@link MethodMetadata} of the method.
		 * @return the method metadata.
		 */
		public MethodMetadata getMethodMetadata() {
			return this.methodMetadata;
		}

		/**
		 * Return the {@link ConditionOutcome}. Note! {@link ConditionOutcome#isMatch()}
		 * always false.
		 * @return outcome for a condition match.
		 */
		public ConditionOutcome getConditionOutcome() {
			return this.conditionOutcome;
		}

	}

	/**
	 * A bean report, contains a base information about a bean.
	 */
	protected static final class BeanReport {

		private final String name;

		private final boolean nullBean;

		private final BeanDefinition definition;

		public BeanReport(String name, boolean nullBean, BeanDefinition definition) {
			this.name = name;
			this.nullBean = nullBean;
			this.definition = definition;
		}

		/**
		 * Return the bean name.
		 * @return the bean name.
		 */
		public String getName() {
			return this.name;
		}

		/**
		 * Return whether bean is {@code NullBean} or not.
		 * @return whether bean is {@code NullBean} or not.
		 */
		public boolean isNullBean() {
			return this.nullBean;
		}

		/**
		 * Return the {@code BeanDefinition} for the a bean.
		 * @return the {@code BeanDefinition}.
		 */
		public BeanDefinition getDefinition() {
			return this.definition;
		}

	}

	/**
	 * A bean metadata which should be inspected.
	 */
	protected static final class BeanMetadata {

		private final String name;

		private final ResolvableType type;

		public BeanMetadata(String name) {
			this(name, (ResolvableType) null);
		}

		public BeanMetadata(Class<?> type) {
			this(null, ResolvableType.forClass(type));
		}

		public BeanMetadata(ResolvableType type) {
			this(null, type);
		}

		public BeanMetadata(String name, Class<?> type) {
			this(name, ResolvableType.forClass(type));

		}

		public BeanMetadata(String name, ResolvableType type) {
			this.name = name;
			this.type = type;
		}

		/**
		 * Return the name of the missing bean, or {@code null}.
		 * @return name of the bean.
		 */

		public String getName() {
			return this.name;
		}

		/**
		 * Return the required {@link ResolvableType} of the missing bean, or {@code
		 * null}.
		 * @return type of the bean.
		 */

		public ResolvableType getResolvableType() {
			return this.type;
		}

	}

	private static final class Source {

		private final String className;

		private final String methodName;

		Source(String source) {
			String[] tokens = source.split("#");
			this.className = (tokens.length > 1) ? tokens[0] : source;
			this.methodName = (tokens.length != 2) ? null : tokens[1];
		}

		String getClassName() {
			return this.className;
		}

		String getMethodName() {
			return this.methodName;
		}

		boolean isMethod() {
			return this.methodName != null;
		}

	}

	private static final class BeanMethods implements Iterable<MethodMetadata> {

		private final List<MethodMetadata> methods;

		BeanMethods(Source source, BeanMetadata beanMetadata,
				MetadataReaderFactory metadataReaderFactory, ClassLoader classLoader) {
			this.methods = findBeanMethods(source, beanMetadata, metadataReaderFactory,
					classLoader);
		}

		private List<MethodMetadata> findBeanMethods(Source source,
				BeanMetadata beanMetadata, MetadataReaderFactory metadataReaderFactory,
				ClassLoader classLoader) {
			try {
				MetadataReader classMetadata = metadataReaderFactory
						.getMetadataReader(source.getClassName());
				Set<MethodMetadata> candidates = classMetadata.getAnnotationMetadata()
						.getAnnotatedMethods(Bean.class.getName());
				List<MethodMetadata> result = new ArrayList<>();
				for (MethodMetadata candidate : candidates) {
					if (isMatch(candidate, source, beanMetadata, classLoader)) {
						result.add(candidate);
					}
				}
				return Collections.unmodifiableList(result);
			}
			catch (Throwable ex) {
				return Collections.emptyList();
			}
		}

		private boolean isMatch(MethodMetadata candidate, Source source,
				BeanMetadata beanMetadata, ClassLoader beanClassLoader) {
			if (source.getMethodName() != null
					&& !source.getMethodName().equals(candidate.getMethodName())) {
				return false;
			}
			String name = beanMetadata.getName();
			ResolvableType resolvableType = beanMetadata.getResolvableType();
			return (name != null && hasName(candidate, name)) || (resolvableType != null
					&& hasType(candidate, resolvableType.getRawClass(), beanClassLoader));

		}

		private boolean hasName(MethodMetadata methodMetadata, String name) {
			Map<String, Object> attributes = methodMetadata
					.getAnnotationAttributes(Bean.class.getName());
			String[] candidates = (attributes != null) ? (String[]) attributes.get("name")
					: null;
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

		private boolean hasType(MethodMetadata candidate, Class<?> type,
				ClassLoader classLoader) {
			if (type == null) {
				return false;
			}
			String returnTypeName = candidate.getReturnTypeName();
			if (type.getName().equals(returnTypeName)) {
				return true;
			}
			Class<?> returnType = forName(returnTypeName, classLoader);
			return returnType != null && type.isAssignableFrom(returnType);
		}

		@Override
		public Iterator<MethodMetadata> iterator() {
			return this.methods.iterator();
		}

	}

}
