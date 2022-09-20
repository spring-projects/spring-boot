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

package org.springframework.boot.context.properties;

import java.util.Arrays;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.factory.aot.AotServices;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.context.properties.scan.valid.b.BScanConfiguration;
import org.springframework.boot.context.properties.scan.valid.b.BScanConfiguration.BFirstProperties;
import org.springframework.boot.context.properties.scan.valid.b.BScanConfiguration.BSecondProperties;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.aot.ApplicationContextAotGenerator;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.test.tools.CompileWithForkedClassLoader;
import org.springframework.core.test.tools.TestCompiler;
import org.springframework.javapoet.ClassName;
import org.springframework.test.context.support.TestPropertySourceUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigurationPropertiesBeanRegistrationAotProcessor}.
 *
 * @author Stephane Nicoll
 */
class ConfigurationPropertiesBeanRegistrationAotProcessorTests {

	private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	private final ConfigurationPropertiesBeanRegistrationAotProcessor processor = new ConfigurationPropertiesBeanRegistrationAotProcessor();

	@Test
	void configurationPropertiesBeanRegistrationAotProcessorIsRegistered() {
		assertThat(AotServices.factories().load(BeanRegistrationAotProcessor.class))
				.anyMatch(ConfigurationPropertiesBeanRegistrationAotProcessor.class::isInstance);
	}

	@Test
	void processAheadOfTimeWithNoConfigurationPropertiesBean() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(String.class);
		this.beanFactory.registerBeanDefinition("test", beanDefinition);
		BeanRegistrationAotContribution contribution = this.processor
				.processAheadOfTime(RegisteredBean.of(this.beanFactory, "test"));
		assertThat(contribution).isNull();
	}

	@Test
	void processAheadOfTimeWithJavaBeanConfigurationPropertiesBean() {
		BeanRegistrationAotContribution contribution = process(JavaBeanSampleBean.class);
		assertThat(contribution).isNull();
	}

	@Test
	void processAheadOfTimeWithValueObjectConfigurationPropertiesBean() {
		BeanRegistrationAotContribution contribution = process(ValueObjectSampleBean.class);
		assertThat(contribution).isNotNull();
	}

	private BeanRegistrationAotContribution process(Class<?> type) {
		ConfigurationPropertiesBeanRegistrar beanRegistrar = new ConfigurationPropertiesBeanRegistrar(this.beanFactory);
		beanRegistrar.register(type);
		RegisteredBean registeredBean = RegisteredBean.of(this.beanFactory,
				this.beanFactory.getBeanDefinitionNames()[0]);
		return this.processor.processAheadOfTime(registeredBean);
	}

	@Test
	@CompileWithForkedClassLoader
	void aotContributedInitializerBindsValueObject() {
		compile(createContext(ValueObjectSampleBeanConfiguration.class), (freshContext) -> {
			TestPropertySourceUtils.addInlinedPropertiesToEnvironment(freshContext, "test.name=Hello");
			freshContext.refresh();
			ValueObjectSampleBean bean = freshContext.getBean(ValueObjectSampleBean.class);
			assertThat(bean.name).isEqualTo("Hello");
		});
	}

	@Test
	@CompileWithForkedClassLoader
	void aotContributedInitializerBindsJavaBean() {
		compile(createContext(JavaBeanSampleBeanConfiguration.class), (freshContext) -> {
			TestPropertySourceUtils.addInlinedPropertiesToEnvironment(freshContext, "test.name=Hello");
			freshContext.refresh();
			JavaBeanSampleBean bean = freshContext.getBean(JavaBeanSampleBean.class);
			assertThat(bean.getName()).isEqualTo("Hello");
		});
	}

	@Test
	@CompileWithForkedClassLoader
	void aotContributedInitializerBindsScannedValueObject() {
		compile(createContext(ScanTestConfiguration.class), (freshContext) -> {
			TestPropertySourceUtils.addInlinedPropertiesToEnvironment(freshContext, "b.first.name=Hello");
			freshContext.refresh();
			BFirstProperties bean = freshContext.getBean(BFirstProperties.class);
			assertThat(bean.getName()).isEqualTo("Hello");
		});
	}

	@Test
	@CompileWithForkedClassLoader
	void aotContributedInitializerBindsScannedJavaBean() {
		compile(createContext(ScanTestConfiguration.class), (freshContext) -> {
			TestPropertySourceUtils.addInlinedPropertiesToEnvironment(freshContext, "b.second.number=42");
			freshContext.refresh();
			BSecondProperties bean = freshContext.getBean(BSecondProperties.class);
			assertThat(bean.getNumber()).isEqualTo(42);
		});
	}

	private GenericApplicationContext createContext(Class<?>... types) {
		GenericApplicationContext context = new AnnotationConfigApplicationContext();
		context.registerBean(JavaBeanSampleBeanConfiguration.class);
		Arrays.stream(types).forEach((type) -> context.registerBean(type));
		return context;
	}

	@SuppressWarnings("unchecked")
	private void compile(GenericApplicationContext context, Consumer<GenericApplicationContext> freshContext) {
		TestGenerationContext generationContext = new TestGenerationContext(TestTarget.class);
		ClassName className = new ApplicationContextAotGenerator().processAheadOfTime(context, generationContext);
		generationContext.writeGeneratedContent();
		TestCompiler.forSystem().with(generationContext).compile((compiled) -> {
			GenericApplicationContext freshApplicationContext = new GenericApplicationContext();
			ApplicationContextInitializer<GenericApplicationContext> initializer = compiled
					.getInstance(ApplicationContextInitializer.class, className.toString());
			initializer.initialize(freshApplicationContext);
			freshContext.accept(freshApplicationContext);
		});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(JavaBeanSampleBean.class)
	static class JavaBeanSampleBeanConfiguration {

	}

	@ConfigurationProperties("test")
	public static class JavaBeanSampleBean {

		private String name;

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(ValueObjectSampleBean.class)
	static class ValueObjectSampleBeanConfiguration {

	}

	@ConfigurationProperties("test")
	public static class ValueObjectSampleBean {

		@SuppressWarnings("unused")
		private final String name;

		ValueObjectSampleBean(String name) {
			this.name = name;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConfigurationPropertiesScan(basePackageClasses = BScanConfiguration.class)
	static class ScanTestConfiguration {

	}

	static class TestTarget {

	}

}
