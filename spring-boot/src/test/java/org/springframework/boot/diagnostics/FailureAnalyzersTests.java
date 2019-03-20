/*
 * Copyright 2012-2017 the original author or authors.
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

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link FailureAnalyzers}.
 *
 * @author Andy Wilkinson
 */
public class FailureAnalyzersTests {

	private static BeanFactoryAwareFailureAnalyzer failureAnalyzer;

	@Before
	public void configureMock() {
		failureAnalyzer = mock(BeanFactoryAwareFailureAnalyzer.class);
	}

	@Test
	public void analyzersAreLoadedAndCalled() {
		RuntimeException failure = new RuntimeException();
		analyzeAndReport("basic.factories", failure);
		verify(failureAnalyzer, times(2)).analyze(failure);
	}

	@Test
	public void beanFactoryIsInjectedIntoBeanFactoryAwareFailureAnalyzers() {
		RuntimeException failure = new RuntimeException();
		analyzeAndReport("basic.factories", failure);
		verify(failureAnalyzer).setBeanFactory(any(BeanFactory.class));
	}

	@Test
	public void analyzerThatFailsDuringInitializationDoesNotPreventOtherAnalyzersFromBeingCalled() {
		RuntimeException failure = new RuntimeException();
		analyzeAndReport("broken-initialization.factories", failure);
		verify(failureAnalyzer, times(1)).analyze(failure);
	}

	@Test
	public void analyzerThatFailsDuringAnalysisDoesNotPreventOtherAnalyzersFromBeingCalled() {
		RuntimeException failure = new RuntimeException();
		analyzeAndReport("broken-analysis.factories", failure);
		verify(failureAnalyzer, times(1)).analyze(failure);
	}

	private void analyzeAndReport(String factoriesName, Throwable failure) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		ClassLoader classLoader = new CustomSpringFactoriesClassLoader(factoriesName);
		new FailureAnalyzers(context, classLoader).analyzeAndReport(failure);
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

	interface BeanFactoryAwareFailureAnalyzer extends BeanFactoryAware, FailureAnalyzer {

	}

	static class StandardBeanFactoryAwareFailureAnalyzer extends BasicFailureAnalyzer
			implements BeanFactoryAwareFailureAnalyzer {

		@Override
		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
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
				return super.getResources(
						"failure-analyzers-tests/" + this.factoriesName);
			}
			return super.getResources(name);
		}

	}

}
