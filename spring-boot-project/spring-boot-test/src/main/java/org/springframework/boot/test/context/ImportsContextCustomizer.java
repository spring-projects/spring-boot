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

package org.springframework.boot.test.context;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.context.annotation.DeterminableImports;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.core.style.ToStringCreator;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.util.ReflectionUtils;

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
		AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(registry);
		registerCleanupPostProcessor(registry, reader);
		registerImportsConfiguration(registry, reader);
	}

	private void registerCleanupPostProcessor(BeanDefinitionRegistry registry, AnnotatedBeanDefinitionReader reader) {
		BeanDefinition definition = registerBean(registry, reader, ImportsCleanupPostProcessor.BEAN_NAME,
				ImportsCleanupPostProcessor.class);
		definition.getConstructorArgumentValues().addIndexedArgumentValue(0, this.testClass);
	}

	private void registerImportsConfiguration(BeanDefinitionRegistry registry, AnnotatedBeanDefinitionReader reader) {
		BeanDefinition definition = registerBean(registry, reader, ImportsConfiguration.BEAN_NAME,
				ImportsConfiguration.class);
		definition.setAttribute(TEST_CLASS_ATTRIBUTE, this.testClass);
	}

	private BeanDefinitionRegistry getBeanDefinitionRegistry(ApplicationContext context) {
		if (context instanceof BeanDefinitionRegistry) {
			return (BeanDefinitionRegistry) context;
		}
		if (context instanceof AbstractApplicationContext) {
			return (BeanDefinitionRegistry) ((AbstractApplicationContext) context).getBeanFactory();
		}
		throw new IllegalStateException("Could not locate BeanDefinitionRegistry");
	}

	private BeanDefinition registerBean(BeanDefinitionRegistry registry, AnnotatedBeanDefinitionReader reader,
			String beanName, Class<?> type) {
		reader.registerBean(type, beanName);
		return registry.getBeanDefinition(beanName);
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

	@Override
	public int hashCode() {
		return this.key.hashCode();
	}

	@Override
	public String toString() {
		return new ToStringCreator(this).append("key", this.key).toString();
	}

	/**
	 * {@link Configuration @Configuration} registered to trigger the
	 * {@link ImportsSelector}.
	 */
	@Configuration(proxyBeanMethods = false)
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
			BeanDefinition definition = this.beanFactory.getBeanDefinition(ImportsConfiguration.BEAN_NAME);
			Object testClass = (definition != null) ? definition.getAttribute(TEST_CLASS_ATTRIBUTE) : null;
			return (testClass != null) ? new String[] { ((Class<?>) testClass).getName() } : NO_IMPORTS;
		}

	}

	/**
	 * {@link BeanDefinitionRegistryPostProcessor} to cleanup temporary configuration
	 * added to load imports.
	 */
	@Order(Ordered.LOWEST_PRECEDENCE)
	static class ImportsCleanupPostProcessor implements BeanDefinitionRegistryPostProcessor {

		static final String BEAN_NAME = ImportsCleanupPostProcessor.class.getName();

		private final Class<?> testClass;

		ImportsCleanupPostProcessor(Class<?> testClass) {
			this.testClass = testClass;
		}

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		}

		@Override
		public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
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

		private static final Class<?>[] NO_IMPORTS = {};

		private static final Set<AnnotationFilter> ANNOTATION_FILTERS;

		static {
			Set<AnnotationFilter> filters = new HashSet<>();
			filters.add(new JavaLangAnnotationFilter());
			filters.add(new KotlinAnnotationFilter());
			filters.add(new SpockAnnotationFilter());
			filters.add(new JunitAnnotationFilter());
			ANNOTATION_FILTERS = Collections.unmodifiableSet(filters);
		}

		private final Set<Object> key;

		ContextCustomizerKey(Class<?> testClass) {
			Set<Annotation> annotations = new HashSet<>();
			Set<Class<?>> seen = new HashSet<>();
			collectClassAnnotations(testClass, annotations, seen);
			Set<Object> determinedImports = determineImports(annotations, testClass);
			this.key = Collections.unmodifiableSet((determinedImports != null) ? determinedImports : annotations);
		}

		private void collectClassAnnotations(Class<?> classType, Set<Annotation> annotations, Set<Class<?>> seen) {
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

		private void collectElementAnnotations(AnnotatedElement element, Set<Annotation> annotations,
				Set<Class<?>> seen) {
			for (Annotation annotation : element.getDeclaredAnnotations()) {
				if (!isIgnoredAnnotation(annotation)) {
					annotations.add(annotation);
					collectClassAnnotations(annotation.annotationType(), annotations, seen);
				}
			}
		}

		private boolean isIgnoredAnnotation(Annotation annotation) {
			for (AnnotationFilter annotationFilter : ANNOTATION_FILTERS) {
				if (annotationFilter.isIgnored(annotation)) {
					return true;
				}
			}
			return false;
		}

		private Set<Object> determineImports(Set<Annotation> annotations, Class<?> testClass) {
			Set<Object> determinedImports = new LinkedHashSet<>();
			AnnotationMetadata testClassMetadata = AnnotationMetadata.introspect(testClass);
			for (Annotation annotation : annotations) {
				for (Class<?> source : getImports(annotation)) {
					Set<Object> determinedSourceImports = determineImports(source, testClassMetadata);
					if (determinedSourceImports == null) {
						return null;
					}
					determinedImports.addAll(determinedSourceImports);
				}
			}
			return determinedImports;
		}

		private Class<?>[] getImports(Annotation annotation) {
			if (annotation instanceof Import) {
				return ((Import) annotation).value();
			}
			return NO_IMPORTS;
		}

		private Set<Object> determineImports(Class<?> source, AnnotationMetadata metadata) {
			if (DeterminableImports.class.isAssignableFrom(source)) {
				// We can determine the imports
				return ((DeterminableImports) instantiate(source)).determineImports(metadata);
			}
			if (ImportSelector.class.isAssignableFrom(source)
					|| ImportBeanDefinitionRegistrar.class.isAssignableFrom(source)) {
				// Standard ImportSelector and ImportBeanDefinitionRegistrar could
				// use anything to determine the imports so we can't be sure
				return null;
			}
			// The source itself is the import
			return Collections.singleton(source.getName());
		}

		@SuppressWarnings("unchecked")
		private <T> T instantiate(Class<T> source) {
			try {
				Constructor<?> constructor = source.getDeclaredConstructor();
				ReflectionUtils.makeAccessible(constructor);
				return (T) constructor.newInstance();
			}
			catch (Throwable ex) {
				throw new IllegalStateException("Unable to instantiate DeterminableImportSelector " + source.getName(),
						ex);
			}
		}

		@Override
		public boolean equals(Object obj) {
			return (obj != null && getClass() == obj.getClass() && this.key.equals(((ContextCustomizerKey) obj).key));
		}

		@Override
		public int hashCode() {
			return this.key.hashCode();
		}

		@Override
		public String toString() {
			return this.key.toString();
		}

	}

	/**
	 * Filter used to limit considered annotations.
	 */
	private interface AnnotationFilter {

		boolean isIgnored(Annotation annotation);

	}

	/**
	 * {@link AnnotationFilter} for {@literal java.lang} annotations.
	 */
	private static final class JavaLangAnnotationFilter implements AnnotationFilter {

		@Override
		public boolean isIgnored(Annotation annotation) {
			return AnnotationUtils.isInJavaLangAnnotationPackage(annotation);
		}

	}

	/**
	 * {@link AnnotationFilter} for Kotlin annotations.
	 */
	private static final class KotlinAnnotationFilter implements AnnotationFilter {

		@Override
		public boolean isIgnored(Annotation annotation) {
			return "kotlin.Metadata".equals(annotation.annotationType().getName())
					|| isInKotlinAnnotationPackage(annotation);
		}

		private boolean isInKotlinAnnotationPackage(Annotation annotation) {
			return annotation.annotationType().getName().startsWith("kotlin.annotation.");
		}

	}

	/**
	 * {@link AnnotationFilter} for Spock annotations.
	 */
	private static final class SpockAnnotationFilter implements AnnotationFilter {

		@Override
		public boolean isIgnored(Annotation annotation) {
			return annotation.annotationType().getName().startsWith("org.spockframework.")
					|| annotation.annotationType().getName().startsWith("spock.");
		}

	}

	/**
	 * {@link AnnotationFilter} for JUnit annotations.
	 */
	private static final class JunitAnnotationFilter implements AnnotationFilter {

		@Override
		public boolean isIgnored(Annotation annotation) {
			return annotation.annotationType().getName().startsWith("org.junit.");
		}

	}

}
