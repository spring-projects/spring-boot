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

package org.springframework.boot.test.context;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;

/**
 * {@link ContextCustomizer} to allow {@code @Import} annotations to be used directly on
 * test classes.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @see ImportsContextCustomizerFactory
 */
class ImportsContextCustomizer implements ContextCustomizer {

	static final String TEST_CLASS_ATTRIBUTE = "testClass";

	private final Class<?> testClass;

	private final ContextCustomizerKey key;

	ImportsContextCustomizer(Class<?> testClass) {
		this.testClass = testClass;
		this.key = new ContextCustomizerKey(testClass);
	}

	@Override
	public void customizeContext(ConfigurableApplicationContext context,
			MergedContextConfiguration mergedContextConfiguration) {
		BeanDefinitionRegistry registry = getBeanDefinitionRegistry(context);
		AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(
				registry);
		registerCleanupPostProcessor(registry, reader);
		registerImportsConfiguration(registry, reader);
	}

	private void registerCleanupPostProcessor(BeanDefinitionRegistry registry,
			AnnotatedBeanDefinitionReader reader) {
		BeanDefinition definition = registerBean(registry, reader,
				ImportsCleanupPostProcessor.BEAN_NAME, ImportsCleanupPostProcessor.class);
		definition.getConstructorArgumentValues().addIndexedArgumentValue(0,
				this.testClass);
	}

	private void registerImportsConfiguration(BeanDefinitionRegistry registry,
			AnnotatedBeanDefinitionReader reader) {
		BeanDefinition definition = registerBean(registry, reader,
				ImportsConfiguration.BEAN_NAME, ImportsConfiguration.class);
		definition.setAttribute(TEST_CLASS_ATTRIBUTE, this.testClass);
	}

	private BeanDefinitionRegistry getBeanDefinitionRegistry(ApplicationContext context) {
		if (context instanceof BeanDefinitionRegistry) {
			return (BeanDefinitionRegistry) context;
		}
		if (context instanceof AbstractApplicationContext) {
			return (BeanDefinitionRegistry) ((AbstractApplicationContext) context)
					.getBeanFactory();
		}
		throw new IllegalStateException("Could not locate BeanDefinitionRegistry");
	}

	@SuppressWarnings("unchecked")
	private BeanDefinition registerBean(BeanDefinitionRegistry registry,
			AnnotatedBeanDefinitionReader reader, String beanName, Class<?> type) {
		reader.registerBean(type, beanName);
		BeanDefinition definition = registry.getBeanDefinition(beanName);
		return definition;
	}

	@Override
	public int hashCode() {
		return this.key.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj == null || obj.getClass() != getClass()) {
			return false;
		}
		// ImportSelectors are flexible so the only safe cache key is the test class
		ImportsContextCustomizer other = (ImportsContextCustomizer) obj;
		return this.key.equals(other.key);
	}

	/**
	 * {@link Configuration} registered to trigger the {@link ImportsSelector}.
	 */
	@Configuration
	@Import(ImportsSelector.class)
	static class ImportsConfiguration {

		static final String BEAN_NAME = ImportsConfiguration.class.getName();

	}

	/**
	 * {@link ImportSelector} that returns the original test class so that direct
	 * {@code @Import} annotations are processed.
	 */
	static class ImportsSelector implements ImportSelector, BeanFactoryAware {

		private static final String[] NO_IMPORTS = {};

		private ConfigurableListableBeanFactory beanFactory;

		@Override
		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
		}

		@Override
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {
			BeanDefinition definition = this.beanFactory
					.getBeanDefinition(ImportsConfiguration.BEAN_NAME);
			Object testClass = (definition == null ? null
					: definition.getAttribute(TEST_CLASS_ATTRIBUTE));
			return (testClass == null ? NO_IMPORTS
					: new String[] { ((Class<?>) testClass).getName() });
		}

	}

	/**
	 * {@link BeanDefinitionRegistryPostProcessor} to cleanup temporary configuration
	 * added to load imports.
	 */
	@Order(Ordered.LOWEST_PRECEDENCE)
	static class ImportsCleanupPostProcessor
			implements BeanDefinitionRegistryPostProcessor {

		static final String BEAN_NAME = ImportsCleanupPostProcessor.class.getName();

		private final Class<?> testClass;

		ImportsCleanupPostProcessor(Class<?> testClass) {
			this.testClass = testClass;
		}

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
				throws BeansException {
		}

		@Override
		public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry)
				throws BeansException {
			try {
				String[] names = registry.getBeanDefinitionNames();
				for (String name : names) {
					BeanDefinition definition = registry.getBeanDefinition(name);
					if (this.testClass.getName().equals(definition.getBeanClassName())) {
						registry.removeBeanDefinition(name);
					}
				}
				registry.removeBeanDefinition(ImportsConfiguration.BEAN_NAME);
			}
			catch (NoSuchBeanDefinitionException ex) {
			}
		}

	}

	/**
	 * The key used to ensure correct application context caching. Keys are generated
	 * based on <em>all</em> the annotations used with the test that aren't core Java or
	 * Kotlin annotations. We must use something broader than just {@link Import @Import}
	 * annotations since an {@code @Import} may use an {@link ImportSelector} which could
	 * make decisions based on anything available from {@link AnnotationMetadata}.
	 */
	static class ContextCustomizerKey {

		private final Set<Annotation> annotations;

		ContextCustomizerKey(Class<?> testClass) {
			Set<Annotation> annotations = new HashSet<Annotation>();
			Set<Class<?>> seen = new HashSet<Class<?>>();
			collectClassAnnotations(testClass, annotations, seen);
			this.annotations = Collections.unmodifiableSet(annotations);
		}

		private void collectClassAnnotations(Class<?> classType,
				Set<Annotation> annotations, Set<Class<?>> seen) {
			if (seen.add(classType)) {
				collectElementAnnotations(classType, annotations, seen);
				for (Class<?> interfaceType : classType.getInterfaces()) {
					collectClassAnnotations(interfaceType, annotations, seen);
				}
				if (classType.getSuperclass() != null) {
					collectClassAnnotations(classType.getSuperclass(), annotations, seen);
				}
			}
		}

		private void collectElementAnnotations(AnnotatedElement element,
				Set<Annotation> annotations, Set<Class<?>> seen) {
			for (Annotation annotation : element.getDeclaredAnnotations()) {
				if (!AnnotationUtils.isInJavaLangAnnotationPackage(annotation)
						&& !isIgnoredKotlinAnnotation(annotation)) {
					annotations.add(annotation);
					collectClassAnnotations(annotation.annotationType(), annotations,
							seen);
				}
			}
		}

		private boolean isIgnoredKotlinAnnotation(Annotation annotation) {
			return "kotlin.Metadata".equals(annotation.annotationType().getName())
					|| isInKotlinAnnotationPackage(annotation);
		}

		private boolean isInKotlinAnnotationPackage(Annotation annotation) {
			return annotation.annotationType().getName().startsWith("kotlin.annotation.");
		}

		@Override
		public int hashCode() {
			return this.annotations.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			return (obj != null && getClass().equals(obj.getClass())
					&& this.annotations.equals(((ContextCustomizerKey) obj).annotations));
		}

	}

}
