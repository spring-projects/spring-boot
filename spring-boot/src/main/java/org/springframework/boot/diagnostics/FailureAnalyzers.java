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

package org.springframework.boot.diagnostics;

import java.util.List;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.support.SpringFactoriesLoader;

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
 * @since 1.4.0
 */
public final class FailureAnalyzers {

	private FailureAnalyzers() {
	}

	public static boolean analyzeAndReport(Throwable failure, ClassLoader classLoader,
			ConfigurableApplicationContext context) {
		List<FailureAnalyzer> analyzers = SpringFactoriesLoader
				.loadFactories(FailureAnalyzer.class, classLoader);
		List<FailureAnalysisReporter> reporters = SpringFactoriesLoader
				.loadFactories(FailureAnalysisReporter.class, classLoader);
		FailureAnalysis analysis = analyze(failure, analyzers, context);
		return report(analysis, reporters);
	}

	private static FailureAnalysis analyze(Throwable failure,
			List<FailureAnalyzer> analyzers, ConfigurableApplicationContext context) {
		for (FailureAnalyzer analyzer : analyzers) {
			prepareAnalyzer(context, analyzer);
			FailureAnalysis analysis = analyzer.analyze(failure);
			if (analysis != null) {
				return analysis;
			}
		}
		return null;
	}

	private static void prepareAnalyzer(ConfigurableApplicationContext context,
			FailureAnalyzer analyzer) {
		if (analyzer instanceof BeanFactoryAware) {
			((BeanFactoryAware) analyzer).setBeanFactory(context.getBeanFactory());
		}
	}

	private static boolean report(FailureAnalysis analysis,
			List<FailureAnalysisReporter> reporters) {
		if (analysis == null || reporters.isEmpty()) {
			return false;
		}
		for (FailureAnalysisReporter reporter : reporters) {
			reporter.report(analysis);
		}
		return true;
	}

}
