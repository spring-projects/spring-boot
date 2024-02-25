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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationFilter;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
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
 * @author Laurent Martelli
 * @see ImportsContextCustomizerFactory
 */
class ImportsContextCustomizer implements ContextCustomizer {

	private static final String TEST_CLASS_NAME_ATTRIBUTE = "testClassName";

	private final String testClassName;

	private final ContextCustomizerKey key;

	/**
     * Constructs a new ImportsContextCustomizer with the specified test class.
     * 
     * @param testClass the test class for which the context customizer is being created
     */
    ImportsContextCustomizer(Class<?> testClass) {
		this.testClassName = testClass.getName();
		this.key = new ContextCustomizerKey(testClass);
	}

	/**
     * Customizes the application context by registering bean definitions and configurations.
     * 
     * @param context the configurable application context
     * @param mergedContextConfiguration the merged context configuration
     */
    @Override
	public void customizeContext(ConfigurableApplicationContext context,
			MergedContextConfiguration mergedContextConfiguration) {
		BeanDefinitionRegistry registry = getBeanDefinitionRegistry(context);
		AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(registry);
		registerCleanupPostProcessor(registry, reader);
		registerImportsConfiguration(registry, reader);
	}

	/**
     * Registers a cleanup post processor for the given bean definition registry and annotated bean definition reader.
     * This post processor is responsible for cleaning up imports after the test execution.
     *
     * @param registry the bean definition registry to register the cleanup post processor with
     * @param reader the annotated bean definition reader to use for registering the cleanup post processor
     */
    private void registerCleanupPostProcessor(BeanDefinitionRegistry registry, AnnotatedBeanDefinitionReader reader) {
		BeanDefinition definition = registerBean(registry, reader, ImportsCleanupPostProcessor.BEAN_NAME,
				ImportsCleanupPostProcessor.class);
		definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		definition.getConstructorArgumentValues().addIndexedArgumentValue(0, this.testClassName);
	}

	/**
     * Registers the imports configuration in the given bean definition registry using the provided annotated bean definition reader.
     * 
     * @param registry the bean definition registry to register the imports configuration
     * @param reader the annotated bean definition reader to use for registration
     */
    private void registerImportsConfiguration(BeanDefinitionRegistry registry, AnnotatedBeanDefinitionReader reader) {
		BeanDefinition definition = registerBean(registry, reader, ImportsConfiguration.BEAN_NAME,
				ImportsConfiguration.class);
		definition.setAttribute(TEST_CLASS_NAME_ATTRIBUTE, this.testClassName);
	}

	/**
     * Returns the BeanDefinitionRegistry from the given ApplicationContext.
     * 
     * @param context the ApplicationContext from which to retrieve the BeanDefinitionRegistry
     * @return the BeanDefinitionRegistry if found in the ApplicationContext
     * @throws IllegalStateException if the BeanDefinitionRegistry cannot be located
     */
    private BeanDefinitionRegistry getBeanDefinitionRegistry(ApplicationContext context) {
		if (context instanceof BeanDefinitionRegistry beanDefinitionRegistry) {
			return beanDefinitionRegistry;
		}
		if (context instanceof AbstractApplicationContext abstractContext) {
			return (BeanDefinitionRegistry) abstractContext.getBeanFactory();
		}
		throw new IllegalStateException("Could not locate BeanDefinitionRegistry");
	}

	/**
     * Registers a bean with the given bean name and type in the provided bean definition registry.
     * 
     * @param registry the bean definition registry to register the bean with
     * @param reader the annotated bean definition reader to use for registration
     * @param beanName the name of the bean to register
     * @param type the type of the bean to register
     * @return the bean definition of the registered bean
     */
    private BeanDefinition registerBean(BeanDefinitionRegistry registry, AnnotatedBeanDefinitionReader reader,
			String beanName, Class<?> type) {
		reader.registerBean(type, beanName);
		return registry.getBeanDefinition(beanName);
	}

	/**
     * Compares this ImportsContextCustomizer object with the specified object for equality.
     * 
     * @param obj the object to compare with
     * @return true if the specified object is equal to this ImportsContextCustomizer object, false otherwise
     */
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
     * Returns the hash code value for this ImportsContextCustomizer object.
     * The hash code is generated based on the key of the object.
     *
     * @return the hash code value for this object
     */
    @Override
	public int hashCode() {
		return this.key.hashCode();
	}

	/**
     * Returns a string representation of the object.
     * 
     * @return a string representation of the object
     */
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

		/**
         * Set the BeanFactory that this object runs in.
         * <p>
         * Invoked after population of normal bean properties but before an init callback such as InitializingBean's
         * {@code afterPropertiesSet} or a custom init-method. Invoked after ResourceLoaderAware's {@code setResourceLoader},
         * ApplicationEventPublisherAware's {@code setApplicationEventPublisher} and MessageSourceAware's
         * {@code setMessageSource}.
         * <p>
         * This method will be invoked after any bean properties have been set and before any custom init-method or
         * afterPropertiesSet() callbacks have been invoked.
         * <p>
         * This method allows the bean instance to perform initialization only possible when all bean properties have been set
         * and to throw an exception in the event of misconfiguration.
         * @param beanFactory the BeanFactory object that this object runs in
         * @throws BeansException if initialization attempted by this object fails
         */
        @Override
		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
		}

		/**
         * Selects the imports to be used by the importing class.
         * 
         * @param importingClassMetadata the metadata of the importing class
         * @return an array of import strings
         */
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

		/**
         * Constructs a new ImportsCleanupPostProcessor with the specified test class name.
         * 
         * @param testClassName the name of the test class
         */
        ImportsCleanupPostProcessor(String testClassName) {
			this.testClassName = testClassName;
		}

		/**
         * This method is called after the bean factory has been initialized and all bean definitions have been loaded.
         * It allows for post-processing of the bean factory, before beans are actually instantiated.
         * 
         * @param beanFactory the bean factory that has been initialized
         * @throws BeansException if any error occurs during the post-processing
         */
        @Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		}

		/**
         * This method is used to post-process the bean definition registry by removing specific bean definitions.
         * It iterates through all the bean definitions in the registry and removes the ones that have a matching class name.
         * Additionally, it also removes a specific bean definition by its name.
         * 
         * @param registry The bean definition registry to be processed.
         * @throws BeansException If an error occurs during the bean processing.
         */
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
				// Ignore
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

		private static final Set<AnnotationFilter> ANNOTATION_FILTERS;
		static {
			Set<AnnotationFilter> annotationFilters = new LinkedHashSet<>();
			annotationFilters.add(AnnotationFilter.PLAIN);
			annotationFilters.add("kotlin.Metadata"::equals);
			annotationFilters.add(AnnotationFilter.packages("kotlin.annotation"));
			annotationFilters.add(AnnotationFilter.packages("org.spockframework", "spock"));
			annotationFilters.add(AnnotationFilter.packages("org.junit"));
			ANNOTATION_FILTERS = Collections.unmodifiableSet(annotationFilters);
		}
		private final Set<Object> key;

		/**
         * Constructs a new ContextCustomizerKey object for the given test class.
         * 
         * @param testClass the test class for which the ContextCustomizerKey is being created
         */
        ContextCustomizerKey(Class<?> testClass) {
			MergedAnnotations annotations = MergedAnnotations.search(MergedAnnotations.SearchStrategy.TYPE_HIERARCHY)
				.withAnnotationFilter(this::isFilteredAnnotation)
				.from(testClass);
			Set<Object> determinedImports = determineImports(annotations, testClass);
			if (determinedImports == null) {
				this.key = Collections.unmodifiableSet(synthesize(annotations));
			}
			else {
				Set<Object> key = new HashSet<>();
				key.addAll(determinedImports);
				Set<Annotation> componentScanning = annotations.stream()
					.filter((annotation) -> annotation.getType().equals(ComponentScan.class))
					.map(MergedAnnotation::synthesize)
					.collect(Collectors.toSet());
				key.addAll(componentScanning);
				this.key = Collections.unmodifiableSet(key);
			}
		}

		/**
         * Checks if the given type name matches any of the annotation filters.
         *
         * @param typeName the type name to be checked
         * @return true if the type name matches any of the annotation filters, false otherwise
         */
        private boolean isFilteredAnnotation(String typeName) {
			return ANNOTATION_FILTERS.stream().anyMatch((filter) -> filter.matches(typeName));
		}

		/**
         * Determines the imports required for the given merged annotations and test class.
         * 
         * @param annotations the merged annotations to process
         * @param testClass the test class to introspect
         * @return a set of determined imports
         */
        private Set<Object> determineImports(MergedAnnotations annotations, Class<?> testClass) {
			Set<Object> determinedImports = new LinkedHashSet<>();
			AnnotationMetadata metadata = AnnotationMetadata.introspect(testClass);
			for (MergedAnnotation<Import> annotation : annotations.stream(Import.class).toList()) {
				for (Class<?> source : annotation.getClassArray(MergedAnnotation.VALUE)) {
					Set<Object> determinedSourceImports = determineImports(source, metadata);
					if (determinedSourceImports == null) {
						return null;
					}
					determinedImports.addAll(determinedSourceImports);
				}
			}
			return determinedImports;
		}

		/**
         * Determines the imports for a given source class and annotation metadata.
         * 
         * @param source the source class
         * @param metadata the annotation metadata
         * @return a set of objects representing the imports, or null if the imports cannot be determined
         */
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

		/**
         * Synthesizes a set of objects from the given merged annotations.
         *
         * @param annotations the merged annotations to synthesize from
         * @return a set of synthesized objects
         */
        private Set<Object> synthesize(MergedAnnotations annotations) {
			return annotations.stream().map(MergedAnnotation::synthesize).collect(Collectors.toSet());
		}

		/**
         * Instantiates an object of the given source class.
         * 
         * @param source the class to instantiate
         * @return the instantiated object
         * @throws IllegalStateException if unable to instantiate the object
         */
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

		/**
         * Compares this ContextCustomizerKey object to the specified object. The result is true if and only if the argument is not null, is of the same class as this object, and has the same key value.
         *
         * @param obj the object to compare this ContextCustomizerKey against
         * @return true if the given object represents a ContextCustomizerKey with the same key value, false otherwise
         */
        @Override
		public boolean equals(Object obj) {
			return (obj != null && getClass() == obj.getClass() && this.key.equals(((ContextCustomizerKey) obj).key));
		}

		/**
         * Returns the hash code value for this ContextCustomizerKey object.
         * 
         * @return the hash code value for this ContextCustomizerKey object
         */
        @Override
		public int hashCode() {
			return this.key.hashCode();
		}

		/**
         * Returns a string representation of the object.
         * 
         * @return the string representation of the object
         */
        @Override
		public String toString() {
			return this.key.toString();
		}

	}

}
