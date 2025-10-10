/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.diagnostics.analyzer;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.analyzer.nounique.TestBean;
import org.springframework.boot.diagnostics.analyzer.nounique.TestBeanConsumer;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NoUniqueBeanDefinitionFailureAnalyzer}.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
@WithResource(name = "producer.xml",
		content = """
				<?xml version="1.0" encoding="UTF-8"?>
				<beans xmlns="http://www.springframework.org/schema/beans"
					xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
					xsi:schemaLocation="http://www.springframework.org/schema/beans https://www.springframework.org/schema/beans/spring-beans.xsd">

					<bean class="org.springframework.boot.diagnostics.analyzer.nounique.TestBean" name="xmlTestBean"/>

				</beans>
				""")
class NoUniqueBeanDefinitionFailureAnalyzerTests {

	private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	private final NoUniqueBeanDefinitionFailureAnalyzer analyzer = new NoUniqueBeanDefinitionFailureAnalyzer(
			this.context.getBeanFactory());

	@Test
	void failureAnalysisForFieldConsumer() {
		FailureAnalysis failureAnalysis = analyzeFailure(createFailure(FieldConsumer.class));
		assertThat(failureAnalysis.getDescription()).startsWith(
				"Field testBean in " + FieldConsumer.class.getName() + " required a single bean, but 6 were found:");
		assertFoundBeans(failureAnalysis);
	}

	@Test
	void failureAnalysisForMethodConsumer() {
		FailureAnalysis failureAnalysis = analyzeFailure(createFailure(MethodConsumer.class));
		assertThat(failureAnalysis.getDescription()).startsWith("Parameter 0 of method consumer in "
				+ MethodConsumer.class.getName() + " required a single bean, but 6 were found:");
		assertFoundBeans(failureAnalysis);
	}

	@Test
	void failureAnalysisForConstructorConsumer() {
		FailureAnalysis failureAnalysis = analyzeFailure(createFailure(ConstructorConsumer.class));
		assertThat(failureAnalysis.getDescription()).startsWith("Parameter 0 of constructor in "
				+ ConstructorConsumer.class.getName() + " required a single bean, but 6 were found:");
		assertFoundBeans(failureAnalysis);
	}

	@Test
	void failureAnalysisForObjectProviderMethodConsumer() {
		FailureAnalysis failureAnalysis = analyzeFailure(createFailure(ObjectProviderMethodConsumer.class));
		assertThat(failureAnalysis.getDescription()).startsWith("Method consumer in "
				+ ObjectProviderMethodConsumer.class.getName() + " required a single bean, but 6 were found:");
		assertFoundBeans(failureAnalysis);
	}

	@Test
	@WithResource(name = "consumer.xml",
			content = """
					<?xml version="1.0" encoding="UTF-8"?>
					<beans xmlns="http://www.springframework.org/schema/beans"
						xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
						xsi:schemaLocation="http://www.springframework.org/schema/beans https://www.springframework.org/schema/beans/spring-beans.xsd">

						<bean class="org.springframework.boot.diagnostics.analyzer.nounique.TestBeanConsumer"/>

					</beans>
					""")
	void failureAnalysisForXmlConsumer() {
		FailureAnalysis failureAnalysis = analyzeFailure(createFailure(XmlConsumer.class));
		assertThat(failureAnalysis.getDescription()).startsWith("Parameter 0 of constructor in "
				+ TestBeanConsumer.class.getName() + " required a single bean, but 6 were found:");
		assertFoundBeans(failureAnalysis);
	}

	@Test
	void failureAnalysisForObjectProviderConstructorConsumer() {
		FailureAnalysis failureAnalysis = analyzeFailure(createFailure(ObjectProviderConstructorConsumer.class));
		assertThat(failureAnalysis.getDescription()).startsWith("Constructor in "
				+ ObjectProviderConstructorConsumer.class.getName() + " required a single bean, but 6 were found:");
		assertFoundBeans(failureAnalysis);
	}

	@Test
	void failureAnalysisIncludesPossiblyMissingParameterNames() {
		FailureAnalysis failureAnalysis = analyzeFailure(createFailure(MethodConsumer.class));
		assertThat(failureAnalysis.getDescription()).contains(MissingParameterNamesFailureAnalyzer.POSSIBILITY);
		assertThat(failureAnalysis.getAction()).contains(MissingParameterNamesFailureAnalyzer.ACTION);
		assertFoundBeans(failureAnalysis);
	}

	@Test
	void failureAnalysisWithoutInjectionPoints() {
		this.context.registerBean("beanOne", TestBean.class);
		this.context.register(DuplicateBeansProducer.class);
		this.context.refresh();
		FailureAnalysis failureAnalysis = analyzeFailure(new NoUniqueBeanDefinitionException(TestBean.class, 3,
				"no TestBeanProvider specified and expected single matching TestBean but found 3: beanOne,beanTwo,xmlBean"));
		assertThat(failureAnalysis.getDescription())
			.startsWith("A component required a single bean, but 3 were found:");
		assertThat(failureAnalysis.getDescription()).contains("beanOne: defined in unknown location");
		assertThat(failureAnalysis.getDescription())
			.contains("beanTwo: defined by method 'beanTwo' in " + DuplicateBeansProducer.class.getName());
		assertThat(failureAnalysis.getDescription()).contains("xmlBean: a programmatically registered singleton");
	}

	private BeanCreationException createFailure(Class<?> consumer) {
		this.context.registerBean("beanOne", TestBean.class);
		this.context.register(DuplicateBeansProducer.class, consumer);
		this.context.setParent(new AnnotationConfigApplicationContext(ParentProducer.class));
		try {
			this.context.refresh();
			throw new AssertionError("Should not be reached");
		}
		catch (BeanCreationException ex) {
			return ex;
		}
	}

	private FailureAnalysis analyzeFailure(Exception failure) {
		FailureAnalysis analyze = this.analyzer.analyze(failure);
		assertThat(analyze).isNotNull();
		return analyze;
	}

	private void assertFoundBeans(FailureAnalysis analysis) {
		assertThat(analysis.getDescription()).contains("beanOne: defined in unknown location");
		assertThat(analysis.getDescription())
			.contains("beanTwo: defined by method 'beanTwo' in " + DuplicateBeansProducer.class.getName());
		assertThat(analysis.getDescription())
			.contains("beanThree: defined by method 'beanThree' in " + ParentProducer.class.getName());
		assertThat(analysis.getDescription()).contains("barTestBean");
		assertThat(analysis.getDescription()).contains("fooTestBean");
		assertThat(analysis.getDescription()).contains("xmlTestBean");
	}

	@Configuration(proxyBeanMethods = false)
	@ComponentScan(basePackageClasses = TestBean.class)
	@ImportResource("classpath:producer.xml")
	static class DuplicateBeansProducer {

		@Bean
		TestBean beanTwo() {
			return new TestBean();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ParentProducer {

		@Bean
		TestBean beanThree() {
			return new TestBean();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class FieldConsumer {

		@SuppressWarnings("unused")
		@Autowired
		private TestBean testBean;

	}

	@Configuration(proxyBeanMethods = false)
	static class ObjectProviderConstructorConsumer {

		ObjectProviderConstructorConsumer(ObjectProvider<TestBean> objectProvider) {
			objectProvider.getIfAvailable();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ConstructorConsumer {

		ConstructorConsumer(TestBean testBean) {

		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MethodConsumer {

		@Bean
		String consumer(TestBean testBean) {
			return "foo";
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ObjectProviderMethodConsumer {

		@Bean
		String consumer(ObjectProvider<TestBean> testBeanProvider) {
			testBeanProvider.getIfAvailable();
			return "foo";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ImportResource("classpath:consumer.xml")
	static class XmlConsumer {

	}

}
