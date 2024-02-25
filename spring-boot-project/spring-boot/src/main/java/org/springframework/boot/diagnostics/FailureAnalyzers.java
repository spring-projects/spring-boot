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

package org.springframework.boot.diagnostics;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.SpringBootExceptionReporter;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.io.support.SpringFactoriesLoader.ArgumentResolver;
import org.springframework.core.io.support.SpringFactoriesLoader.FailureHandler;
import org.springframework.core.log.LogMessage;

/**
 * Utility to trigger {@link FailureAnalyzer} and {@link FailureAnalysisReporter}
 * instances loaded from {@code spring.factories}.
 * <p>
 * A {@code FailureAnalyzer} that requires access to the {@link BeanFactory} or
 * {@link Environment} in order to perform its analysis can implement a constructor that
 * accepts arguments of one or both of these types.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Scott Frederick
 */
final class FailureAnalyzers implements SpringBootExceptionReporter {

	private static final Log logger = LogFactory.getLog(FailureAnalyzers.class);

	private final SpringFactoriesLoader springFactoriesLoader;

	private final List<FailureAnalyzer> analyzers;

	/**
     * Constructs a new FailureAnalyzers object with the given ConfigurableApplicationContext and default resource location.
     * 
     * @param context the ConfigurableApplicationContext to be used
     */
    public FailureAnalyzers(ConfigurableApplicationContext context) {
		this(context,
				SpringFactoriesLoader.forDefaultResourceLocation((context != null) ? context.getClassLoader() : null));
	}

	/**
     * Constructs a new FailureAnalyzers object with the given ConfigurableApplicationContext and SpringFactoriesLoader.
     * 
     * @param context the ConfigurableApplicationContext to be used
     * @param springFactoriesLoader the SpringFactoriesLoader to be used
     */
    FailureAnalyzers(ConfigurableApplicationContext context, SpringFactoriesLoader springFactoriesLoader) {
		this.springFactoriesLoader = springFactoriesLoader;
		this.analyzers = loadFailureAnalyzers(context, this.springFactoriesLoader);
	}

	/**
     * Loads the failure analyzers from the specified context and spring factories loader.
     * 
     * @param context the configurable application context
     * @param springFactoriesLoader the spring factories loader
     * @return the list of loaded failure analyzers
     */
    private static List<FailureAnalyzer> loadFailureAnalyzers(ConfigurableApplicationContext context,
			SpringFactoriesLoader springFactoriesLoader) {
		return springFactoriesLoader.load(FailureAnalyzer.class, getArgumentResolver(context),
				FailureHandler.logging(logger));
	}

	/**
     * Returns the argument resolver for the given application context.
     * 
     * @param context the configurable application context
     * @return the argument resolver, or null if the context is null
     */
    private static ArgumentResolver getArgumentResolver(ConfigurableApplicationContext context) {
		if (context == null) {
			return null;
		}
		ArgumentResolver argumentResolver = ArgumentResolver.of(BeanFactory.class, context.getBeanFactory());
		argumentResolver = argumentResolver.and(Environment.class, context.getEnvironment());
		return argumentResolver;
	}

	/**
     * Reports an exception by analyzing it using the provided analyzers.
     * 
     * @param failure the Throwable object representing the exception to be reported
     * @return true if the exception was successfully reported, false otherwise
     */
    @Override
	public boolean reportException(Throwable failure) {
		FailureAnalysis analysis = analyze(failure, this.analyzers);
		return report(analysis);
	}

	/**
     * Analyzes the given throwable failure using a list of failure analyzers.
     * 
     * @param failure   the throwable failure to be analyzed
     * @param analyzers the list of failure analyzers to be used for analysis
     * @return the failure analysis result, or null if no analysis result is found
     */
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

	/**
     * Reports the failure analysis using the provided analysis object.
     * 
     * @param analysis the FailureAnalysis object containing the details of the failure
     * @return true if the analysis was reported successfully, false otherwise
     */
    private boolean report(FailureAnalysis analysis) {
		List<FailureAnalysisReporter> reporters = this.springFactoriesLoader.load(FailureAnalysisReporter.class);
		if (analysis == null || reporters.isEmpty()) {
			return false;
		}
		for (FailureAnalysisReporter reporter : reporters) {
			reporter.report(analysis);
		}
		return true;
	}

}
