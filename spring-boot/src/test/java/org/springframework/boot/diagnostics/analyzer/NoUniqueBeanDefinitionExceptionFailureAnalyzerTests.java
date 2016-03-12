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

package org.springframework.boot.diagnostics.analyzer;

import org.junit.Test;

import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.analyzer.nounique.TestBean;
import org.springframework.boot.diagnostics.analyzer.nounique.TestBeanConsumer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NoUniqueBeanDefinitionExceptionFailureAnalyzer}.
 *
 * @author Andy Wilkinson
 */
public class NoUniqueBeanDefinitionExceptionFailureAnalyzerTests {

	private final NoUniqueBeanDefinitionExceptionFailureAnalyzer analyzer = new NoUniqueBeanDefinitionExceptionFailureAnalyzer();

	@Test
	public void failureAnalysisForFieldConsumer() {
		FailureAnalysis failureAnalysis = analyzeFailure(
				createFailure(FieldConsumer.class));
		System.out.println(failureAnalysis.getDescription());
		assertThat(failureAnalysis.getDescription())
				.startsWith("Field 'testBean' in " + FieldConsumer.class.getName()
						+ " required a single bean, but 6 were found:");
		assertFoundBeans(failureAnalysis);
	}

	@Test
	public void failureAnalysisForMethodConsumer() {
		FailureAnalysis failureAnalysis = analyzeFailure(
				createFailure(MethodConsumer.class));
		System.out.println(failureAnalysis.getDescription());
		assertThat(failureAnalysis.getDescription()).startsWith(
				"Parameter 0 of method 'consumer' in " + MethodConsumer.class.getName()
						+ " required a single bean, but 6 were found:");
		assertFoundBeans(failureAnalysis);
	}

	@Test
	public void failureAnalysisForXmlConsumer() {
		FailureAnalysis failureAnalysis = analyzeFailure(
				createFailure(XmlConsumer.class));
		System.out.println(failureAnalysis.getDescription());
		assertThat(failureAnalysis.getDescription()).startsWith(
				"Parameter 0 of constructor in " + TestBeanConsumer.class.getName()
						+ " required a single bean, but 6 were found:");
		assertFoundBeans(failureAnalysis);
	}

	private UnsatisfiedDependencyException createFailure(Class<?> consumer) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(DuplicateBeansProducer.class, consumer);
		context.setParent(new AnnotationConfigApplicationContext(ParentProducer.class));
		try {
			context.refresh();
			return null;
		}
		catch (UnsatisfiedDependencyException ex) {
			this.analyzer.setBeanFactory(context.getBeanFactory());
			return ex;
		}
		finally {
			context.close();
		}
	}

	private FailureAnalysis analyzeFailure(UnsatisfiedDependencyException failure) {
		return this.analyzer.analyze(failure);
	}

	private void assertFoundBeans(FailureAnalysis analysis) {
		assertThat(analysis.getDescription())
				.contains("beanOne: defined by method 'beanOne' in "
						+ DuplicateBeansProducer.class.getName());
		assertThat(analysis.getDescription())
				.contains("beanTwo: defined by method 'beanTwo' in "
						+ DuplicateBeansProducer.class.getName());
		assertThat(analysis.getDescription())
				.contains("beanThree: defined by method 'beanThree' in "
						+ ParentProducer.class.getName());
		assertThat(analysis.getDescription()).contains("barTestBean");
		assertThat(analysis.getDescription()).contains("fooTestBean");
		assertThat(analysis.getDescription()).contains("xmlTestBean");
	}

	@Configuration
	@ComponentScan(basePackageClasses = TestBean.class)
	@ImportResource("/org/springframework/boot/diagnostics/analyzer/nounique/producer.xml")
	static class DuplicateBeansProducer {

		@Bean
		TestBean beanOne() {
			return new TestBean();
		}

		@Bean
		TestBean beanTwo() {
			return new TestBean();
		}

	}

	static class ParentProducer {

		@Bean
		TestBean beanThree() {
			return new TestBean();
		}

	}

	@Configuration
	static class FieldConsumer {

		@SuppressWarnings("unused")
		@Autowired
		private TestBean testBean;

	}

	@Configuration
	static class MethodConsumer {

		@Bean
		String consumer(TestBean testBean) {
			return "foo";
		}

	}

	@Configuration
	@ImportResource("/org/springframework/boot/diagnostics/analyzer/nounique/consumer.xml")
	static class XmlConsumer {

	}

}
