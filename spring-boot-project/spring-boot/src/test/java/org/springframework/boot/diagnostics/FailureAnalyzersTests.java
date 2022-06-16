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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.boot.testsupport.system.CapturedOutput;
import org.springframework.boot.testsupport.system.OutputCaptureExtension;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.mock.MockSpringFactoriesLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

/**
 * Tests for {@link FailureAnalyzers}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Scott Frederick
 */
@ExtendWith(OutputCaptureExtension.class)
class FailureAnalyzersTests {

	private static AwareFailureAnalyzer failureAnalyzer;

	private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@BeforeEach
	void configureMock() {
		failureAnalyzer = mock(AwareFailureAnalyzer.class);
	}

	@Test
	void analyzersAreLoadedAndCalled() {
		RuntimeException failure = new RuntimeException();
		analyzeAndReport(failure, BasicFailureAnalyzer.class, StandardAwareFailureAnalyzer.class);
		then(failureAnalyzer).should(times(2)).analyze(failure);
	}

	@Test
	void analyzerIsConstructedWithBeanFactory(CapturedOutput output) {
		RuntimeException failure = new RuntimeException();
		analyzeAndReport(failure, BasicFailureAnalyzer.class, BeanFactoryConstructorFailureAnalyzer.class);
		then(failureAnalyzer).should(times(2)).analyze(failure);
		assertThat(output).doesNotContain("implement BeanFactoryAware or EnvironmentAware");
	}

	@Test
	void analyzerIsConstructedWithEnvironment(CapturedOutput output) {
		RuntimeException failure = new RuntimeException();
		analyzeAndReport(failure, BasicFailureAnalyzer.class, EnvironmentConstructorFailureAnalyzer.class);
		then(failureAnalyzer).should(times(2)).analyze(failure);
		assertThat(output).doesNotContain("implement BeanFactoryAware or EnvironmentAware");
	}

	@Test
	void beanFactoryIsInjectedIntoBeanFactoryAwareFailureAnalyzers(CapturedOutput output) {
		RuntimeException failure = new RuntimeException();
		analyzeAndReport(failure, BasicFailureAnalyzer.class, StandardAwareFailureAnalyzer.class);
		then(failureAnalyzer).should().setBeanFactory(same(this.context.getBeanFactory()));
		assertThat(output).contains("FailureAnalyzers [" + StandardAwareFailureAnalyzer.class.getName()
				+ "] implement BeanFactoryAware or EnvironmentAware.");
	}

	@Test
	void environmentIsInjectedIntoEnvironmentAwareFailureAnalyzers() {
		RuntimeException failure = new RuntimeException();
		analyzeAndReport(failure, BasicFailureAnalyzer.class, StandardAwareFailureAnalyzer.class);
		then(failureAnalyzer).should().setEnvironment(same(this.context.getEnvironment()));
	}

	@Test
	void analyzerThatFailsDuringInitializationDoesNotPreventOtherAnalyzersFromBeingCalled() {
		RuntimeException failure = new RuntimeException();
		analyzeAndReport(failure, BrokenInitializationFailureAnalyzer.class, BasicFailureAnalyzer.class);
		then(failureAnalyzer).should().analyze(failure);
	}

	@Test
	void analyzerThatFailsDuringAnalysisDoesNotPreventOtherAnalyzersFromBeingCalled() {
		RuntimeException failure = new RuntimeException();
		analyzeAndReport(failure, BrokenAnalysisFailureAnalyzer.class, BasicFailureAnalyzer.class);
		then(failureAnalyzer).should().analyze(failure);
	}

	@SafeVarargs
	private void analyzeAndReport(Throwable failure, Class<? extends FailureAnalyzer>... failureAnalyzerClasses) {
		analyzeAndReport(failure, this.context, failureAnalyzerClasses);
	}

	@SafeVarargs
	private void analyzeAndReport(Throwable failure, AnnotationConfigApplicationContext context,
			Class<? extends FailureAnalyzer>... failureAnalyzerClasses) {
		MockSpringFactoriesLoader loader = new MockSpringFactoriesLoader();
		for (Class<? extends FailureAnalyzer> failureAnalyzerClass : failureAnalyzerClasses) {
			loader.add(FailureAnalyzer.class, failureAnalyzerClass);
		}
		new FailureAnalyzers(context, loader).reportException(failure);
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

	static class BeanFactoryConstructorFailureAnalyzer extends BasicFailureAnalyzer {

		BeanFactoryConstructorFailureAnalyzer(BeanFactory beanFactory) {
			assertThat(beanFactory).isNotNull();
		}

	}

	static class EnvironmentConstructorFailureAnalyzer extends BasicFailureAnalyzer {

		EnvironmentConstructorFailureAnalyzer(Environment environment) {
			assertThat(environment).isNotNull();
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

}
