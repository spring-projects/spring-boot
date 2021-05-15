/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.diagnostics;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.boot.SpringBootExceptionReporter;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.log.LogMessage;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Utility to trigger {@link FailureAnalyzer} and {@link FailureAnalysisReporter}
 * instances loaded from {@code spring.factories}.
 * <p>
 * A {@code FailureAnalyzer} that requires access to the {@link BeanFactory} in order to
 * perform its analysis can implement {@code BeanFactoryAware} to have the
 * {@code BeanFactory} injected prior to {@link FailureAnalyzer#analyze(Throwable)} being
 * called.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
final class FailureAnalyzers implements SpringBootExceptionReporter {

	private static final Log logger = LogFactory.getLog(FailureAnalyzers.class);

	private final ClassLoader classLoader;

	private final List<FailureAnalyzer> analyzers;

	FailureAnalyzers(ConfigurableApplicationContext context) {
		this(context, null);
	}

	FailureAnalyzers(ConfigurableApplicationContext context, ClassLoader classLoader) {
		this.classLoader = (classLoader != null) ? classLoader : getClassLoader(context);
		this.analyzers = loadFailureAnalyzers(context, this.classLoader);
	}

	private ClassLoader getClassLoader(ConfigurableApplicationContext context) {
		return (context != null) ? context.getClassLoader() : null;
	}

	private List<FailureAnalyzer> loadFailureAnalyzers(ConfigurableApplicationContext context,
			ClassLoader classLoader) {
		List<String> classNames = SpringFactoriesLoader.loadFactoryNames(FailureAnalyzer.class, classLoader);
		List<FailureAnalyzer> analyzers = new ArrayList<>();
		for (String className : classNames) {
			try {
				FailureAnalyzer analyzer = createAnalyzer(context, className);
				if (analyzer != null) {
					analyzers.add(analyzer);
				}
			}
			catch (Throwable ex) {
				logger.trace(LogMessage.format("Failed to load %s", className), ex);
			}
		}
		AnnotationAwareOrderComparator.sort(analyzers);
		return analyzers;
	}

	private FailureAnalyzer createAnalyzer(ConfigurableApplicationContext context, String className) throws Exception {
		Constructor<?> constructor = ClassUtils.forName(className, this.classLoader).getDeclaredConstructor();
		ReflectionUtils.makeAccessible(constructor);
		FailureAnalyzer analyzer = (FailureAnalyzer) constructor.newInstance();
		if (analyzer instanceof BeanFactoryAware || analyzer instanceof EnvironmentAware) {
			if (context == null) {
				logger.trace(LogMessage.format("Skipping %s due to missing context", className));
				return null;
			}
			if (analyzer instanceof BeanFactoryAware) {
				((BeanFactoryAware) analyzer).setBeanFactory(context.getBeanFactory());
			}
			if (analyzer instanceof EnvironmentAware) {
				((EnvironmentAware) analyzer).setEnvironment(context.getEnvironment());
			}
		}
		return analyzer;
	}

	@Override
	public boolean reportException(Throwable failure) {
		FailureAnalysis analysis = analyze(failure, this.analyzers);
		return report(analysis, this.classLoader);
	}

	private FailureAnalysis analyze(Throwable failure, List<FailureAnalyzer> analyzers) {
		for (FailureAnalyzer analyzer : analyzers) {
			try {
				FailureAnalysis analysis = analyzer.analyze(failure);
				if (analysis != null) {
					return analysis;
				}
			}
			catch (Throwable ex) {
				logger.trace(LogMessage.format("FailureAnalyzer %s failed", analyzer), ex);
			}
		}
		return null;
	}

	private boolean report(FailureAnalysis analysis, ClassLoader classLoader) {
		List<FailureAnalysisReporter> reporters = SpringFactoriesLoader.loadFactories(FailureAnalysisReporter.class,
				classLoader);
		if (analysis == null || reporters.isEmpty()) {
			return false;
		}
		for (FailureAnalysisReporter reporter : reporters) {
			reporter.report(analysis);
		}
		return true;
	}

}
