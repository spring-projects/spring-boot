/*
 * Copyright 2012-2022 the original author or authors.
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

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.Environment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

/**
 * Tests for {@link FailureAnalyzers}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
class FailureAnalyzersTests {

	private static AwareFailureAnalyzer failureAnalyzer;

	@BeforeEach
	void configureMock() {
		failureAnalyzer = mock(AwareFailureAnalyzer.class);
	}

	@Test
	void analyzersAreLoadedAndCalled() {
		RuntimeException failure = new RuntimeException();
		analyzeAndReport("basic.factories", failure);
		then(failureAnalyzer).should(times(2)).analyze(failure);
	}

	@Test
	void beanFactoryIsInjectedIntoBeanFactoryAwareFailureAnalyzers() {
		RuntimeException failure = new RuntimeException();
		analyzeAndReport("basic.factories", failure);
		then(failureAnalyzer).should().setBeanFactory(any(BeanFactory.class));
	}

	@Test
	void environmentIsInjectedIntoEnvironmentAwareFailureAnalyzers() {
		RuntimeException failure = new RuntimeException();
		analyzeAndReport("basic.factories", failure);
		then(failureAnalyzer).should().setEnvironment(any(Environment.class));
	}

	@Test
	void analyzerThatFailsDuringInitializationDoesNotPreventOtherAnalyzersFromBeingCalled() {
		RuntimeException failure = new RuntimeException();
		analyzeAndReport("broken-initialization.factories", failure);
		then(failureAnalyzer).should().analyze(failure);
	}

	@Test
	void analyzerThatFailsDuringAnalysisDoesNotPreventOtherAnalyzersFromBeingCalled() {
		RuntimeException failure = new RuntimeException();
		analyzeAndReport("broken-analysis.factories", failure);
		then(failureAnalyzer).should().analyze(failure);
	}

	@Test
	void createWithNullContextSkipsAwareAnalyzers() {
		RuntimeException failure = new RuntimeException();
		analyzeAndReport("basic.factories", failure, null);
		then(failureAnalyzer).should().analyze(failure);
	}

	private void analyzeAndReport(String factoriesName, Throwable failure) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		analyzeAndReport(factoriesName, failure, context);
	}

	private void analyzeAndReport(String factoriesName, Throwable failure, AnnotationConfigApplicationContext context) {
		ClassLoader classLoader = new CustomSpringFactoriesClassLoader(factoriesName);
		new FailureAnalyzers(context, classLoader).reportException(failure);
	}

	static class BasicFailureAnalyzer implements FailureAnalyzer {

		@Override
		public FailureAnalysis analyze(Throwable failure) {
			return failureAnalyzer.analyze(failure);
		}

	}

	static class BrokenInitializationFailureAnalyzer implements FailureAnalyzer {

		static {
			Object foo = null;
			foo.toString();
		}

		@Override
		public FailureAnalysis analyze(Throwable failure) {
			return null;
		}

	}

	static class BrokenAnalysisFailureAnalyzer implements FailureAnalyzer {

		@Override
		public FailureAnalysis analyze(Throwable failure) {
			throw new NoClassDefFoundError();
		}

	}

	interface AwareFailureAnalyzer extends BeanFactoryAware, EnvironmentAware, FailureAnalyzer {

	}

	static class StandardAwareFailureAnalyzer extends BasicFailureAnalyzer implements AwareFailureAnalyzer {

		@Override
		public void setEnvironment(Environment environment) {
			failureAnalyzer.setEnvironment(environment);
		}

		@Override
		public void setBeanFactory(BeanFactory beanFactory) {
			failureAnalyzer.setBeanFactory(beanFactory);
		}

	}

	static class CustomSpringFactoriesClassLoader extends ClassLoader {

		private final String factoriesName;

		CustomSpringFactoriesClassLoader(String factoriesName) {
			super(CustomSpringFactoriesClassLoader.class.getClassLoader());
			this.factoriesName = factoriesName;
		}

		@Override
		public Enumeration<URL> getResources(String name) throws IOException {
			if ("META-INF/spring.factories".equals(name)) {
				return super.getResources("failure-analyzers-tests/" + this.factoriesName);
			}
			return super.getResources(name);
		}

	}

}
