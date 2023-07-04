/*
 * Copyright 2012-2023 the original author or authors.
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

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationFilter;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.Order;
import org.springframework.core.style.ToStringCreator;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.util.ReflectionUtils;

import static org.springframework.core.annotation.AnnotationFilter.packages;

/**
 * {@link ContextCustomizer} to allow {@code @Import} annotations to be used directly on
 * test classes.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @see ImportsContextCustomizerFactory
 */
class ImportsContextCustomizer implements ContextCustomizer {

	private static final String TEST_CLASS_NAME_ATTRIBUTE = "testClassName";

	private final String testClassName;

	private final ContextCustomizerKey key;

	ImportsContextCustomizer(Class<?> testClass) {
		this.testClassName = testClass.getName();
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
		definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		definition.getConstructorArgumentValues().addIndexedArgumentValue(0, this.testClassName);
	}

	private void registerImportsConfiguration(BeanDefinitionRegistry registry, AnnotatedBeanDefinitionReader reader) {
		BeanDefinition definition = registerBean(registry, reader, ImportsConfiguration.BEAN_NAME,
				ImportsConfiguration.class);
		definition.setAttribute(TEST_CLASS_NAME_ATTRIBUTE, this.testClassName);
	}

	private BeanDefinitionRegistry getBeanDefinitionRegistry(ApplicationContext context) {
		if (context instanceof BeanDefinitionRegistry beanDefinitionRegistry) {
			return beanDefinitionRegistry;
		}
		if (context instanceof AbstractApplicationContext abstractContext) {
			return (BeanDefinitionRegistry) abstractContext.getBeanFactory();
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
			Object testClassName = definition.getAttribute(TEST_CLASS_NAME_ATTRIBUTE);
			return (testClassName != null) ? new String[] { (String) testClassName } : NO_IMPORTS;
		}

	}

	/**
	 * {@link BeanDefinitionRegistryPostProcessor} to cleanup temporary configuration
	 * added to load imports.
	 */
	@Order(Ordered.LOWEST_PRECEDENCE)
	static class ImportsCleanupPostProcessor implements BeanDefinitionRegistryPostProcessor {

		static final String BEAN_NAME = ImportsCleanupPostProcessor.class.getName();

		private final String testClassName;

		ImportsCleanupPostProcessor(String testClassName) {
			this.testClassName = testClassName;
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
					if (this.testClassName.equals(definition.getBeanClassName())) {
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

		private static final AnnotationFilter ANNOTATION_FILTERS = or(packages("java.lang.annotation"),
				packages("org.spockframework", "spock"),
				or(isEqualTo("kotlin.Metadata"), packages("kotlin.annotation")), packages(("org.junit")));

		private final Object key;

		ContextCustomizerKey(Class<?> testClass) {
			MergedAnnotations mergedAnnotations = MergedAnnotations
				.search(MergedAnnotations.SearchStrategy.TYPE_HIERARCHY)
				.withAnnotationFilter(ANNOTATION_FILTERS)
				.from(testClass);
			Set<Object> determinedImports = determineImports(mergedAnnotations, testClass);
			if (determinedImports != null) {
				this.key = determinedImports;
			}
			else {
				this.key = AnnotatedElementUtils.findAllMergedAnnotations(testClass,
						mergedAnnotations.stream().map(MergedAnnotation::getType).collect(Collectors.toSet()));
			}
		}

		private Set<Object> determineImports(MergedAnnotations mergedAnnotations, Class<?> testClass) {
			AnnotationMetadata testClassMetadata = AnnotationMetadata.introspect(testClass);
			return mergedAnnotations.stream(Import.class)
				.flatMap((ma) -> Stream.of(ma.getClassArray("value")))
				.map((source) -> determineImports(source, testClassMetadata))
				.reduce(new HashSet<>(), (a, b) -> {
					if (a == null || b == null) {
						return null;
					}
					else {
						a.add(b);
						return a;
					}
				});
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

	static AnnotationFilter or(AnnotationFilter... filters) {
		return typeName -> Stream.of(filters).anyMatch(filter -> filter.matches(typeName));
	}

	static AnnotationFilter isEqualTo(String expectedTypeName) {
		return typeName -> typeName.equals(expectedTypeName);
	}

}
