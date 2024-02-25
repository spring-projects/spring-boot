/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.autoconfigure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.Aware;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link DeferredImportSelector} to handle {@link EnableAutoConfiguration
 * auto-configuration}. This class can also be subclassed if a custom variant of
 * {@link EnableAutoConfiguration @EnableAutoConfiguration} is needed.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Madhura Bhave
 * @author Moritz Halbritter
 * @author Scott Frederick
 * @since 1.3.0
 * @see EnableAutoConfiguration
 */
public class AutoConfigurationImportSelector implements DeferredImportSelector, BeanClassLoaderAware,
		ResourceLoaderAware, BeanFactoryAware, EnvironmentAware, Ordered {

	private static final AutoConfigurationEntry EMPTY_ENTRY = new AutoConfigurationEntry();

	private static final String[] NO_IMPORTS = {};

	private static final Log logger = LogFactory.getLog(AutoConfigurationImportSelector.class);

	private static final String PROPERTY_NAME_AUTOCONFIGURE_EXCLUDE = "spring.autoconfigure.exclude";

	private ConfigurableListableBeanFactory beanFactory;

	private Environment environment;

	private ClassLoader beanClassLoader;

	private ResourceLoader resourceLoader;

	private ConfigurationClassFilter configurationClassFilter;

	/**
	 * Selects the imports for auto-configuration based on the provided annotation
	 * metadata.
	 * @param annotationMetadata the annotation metadata of the configuration class
	 * @return an array of fully qualified class names to be imported
	 */
	@Override
	public String[] selectImports(AnnotationMetadata annotationMetadata) {
		if (!isEnabled(annotationMetadata)) {
			return NO_IMPORTS;
		}
		AutoConfigurationEntry autoConfigurationEntry = getAutoConfigurationEntry(annotationMetadata);
		return StringUtils.toStringArray(autoConfigurationEntry.getConfigurations());
	}

	/**
	 * Returns a predicate that filters out strings based on exclusion criteria.
	 * @return the exclusion filter predicate
	 */
	@Override
	public Predicate<String> getExclusionFilter() {
		return this::shouldExclude;
	}

	/**
	 * Determines whether the given configuration class should be excluded based on the
	 * configuration class filter.
	 * @param configurationClassName the fully qualified name of the configuration class
	 * @return {@code true} if the configuration class should be excluded, {@code false}
	 * otherwise
	 */
	private boolean shouldExclude(String configurationClassName) {
		return getConfigurationClassFilter().filter(Collections.singletonList(configurationClassName)).isEmpty();
	}

	/**
	 * Return the {@link AutoConfigurationEntry} based on the {@link AnnotationMetadata}
	 * of the importing {@link Configuration @Configuration} class.
	 * @param annotationMetadata the annotation metadata of the configuration class
	 * @return the auto-configurations that should be imported
	 */
	protected AutoConfigurationEntry getAutoConfigurationEntry(AnnotationMetadata annotationMetadata) {
		if (!isEnabled(annotationMetadata)) {
			return EMPTY_ENTRY;
		}
		AnnotationAttributes attributes = getAttributes(annotationMetadata);
		List<String> configurations = getCandidateConfigurations(annotationMetadata, attributes);
		configurations = removeDuplicates(configurations);
		Set<String> exclusions = getExclusions(annotationMetadata, attributes);
		checkExcludedClasses(configurations, exclusions);
		configurations.removeAll(exclusions);
		configurations = getConfigurationClassFilter().filter(configurations);
		fireAutoConfigurationImportEvents(configurations, exclusions);
		return new AutoConfigurationEntry(configurations, exclusions);
	}

	/**
	 * Returns the import group for the AutoConfigurationImportSelector class.
	 * @return the import group for the AutoConfigurationImportSelector class
	 */
	@Override
	public Class<? extends Group> getImportGroup() {
		return AutoConfigurationGroup.class;
	}

	/**
	 * Determines if the auto-configuration is enabled based on the provided metadata.
	 * @param metadata the annotation metadata to be evaluated
	 * @return true if the auto-configuration is enabled, false otherwise
	 */
	protected boolean isEnabled(AnnotationMetadata metadata) {
		if (getClass() == AutoConfigurationImportSelector.class) {
			return getEnvironment().getProperty(EnableAutoConfiguration.ENABLED_OVERRIDE_PROPERTY, Boolean.class, true);
		}
		return true;
	}

	/**
	 * Return the appropriate {@link AnnotationAttributes} from the
	 * {@link AnnotationMetadata}. By default this method will return attributes for
	 * {@link #getAnnotationClass()}.
	 * @param metadata the annotation metadata
	 * @return annotation attributes
	 */
	protected AnnotationAttributes getAttributes(AnnotationMetadata metadata) {
		String name = getAnnotationClass().getName();
		AnnotationAttributes attributes = AnnotationAttributes.fromMap(metadata.getAnnotationAttributes(name, true));
		Assert.notNull(attributes, () -> "No auto-configuration attributes found. Is " + metadata.getClassName()
				+ " annotated with " + ClassUtils.getShortName(name) + "?");
		return attributes;
	}

	/**
	 * Return the source annotation class used by the selector.
	 * @return the annotation class
	 */
	protected Class<?> getAnnotationClass() {
		return EnableAutoConfiguration.class;
	}

	/**
	 * Return the auto-configuration class names that should be considered. By default,
	 * this method will load candidates using {@link ImportCandidates}.
	 * @param metadata the source metadata
	 * @param attributes the {@link #getAttributes(AnnotationMetadata) annotation
	 * attributes}
	 * @return a list of candidate configurations
	 */
	protected List<String> getCandidateConfigurations(AnnotationMetadata metadata, AnnotationAttributes attributes) {
		List<String> configurations = ImportCandidates.load(AutoConfiguration.class, getBeanClassLoader())
			.getCandidates();
		Assert.notEmpty(configurations,
				"No auto configuration classes found in "
						+ "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports. If you "
						+ "are using a custom packaging, make sure that file is correct.");
		return configurations;
	}

	/**
	 * Checks the excluded classes against the configurations and handles any invalid
	 * excludes.
	 * @param configurations the list of configurations
	 * @param exclusions the set of excluded classes
	 */
	private void checkExcludedClasses(List<String> configurations, Set<String> exclusions) {
		List<String> invalidExcludes = new ArrayList<>(exclusions.size());
		for (String exclusion : exclusions) {
			if (ClassUtils.isPresent(exclusion, getClass().getClassLoader()) && !configurations.contains(exclusion)) {
				invalidExcludes.add(exclusion);
			}
		}
		if (!invalidExcludes.isEmpty()) {
			handleInvalidExcludes(invalidExcludes);
		}
	}

	/**
	 * Handle any invalid excludes that have been specified.
	 * @param invalidExcludes the list of invalid excludes (will always have at least one
	 * element)
	 */
	protected void handleInvalidExcludes(List<String> invalidExcludes) {
		StringBuilder message = new StringBuilder();
		for (String exclude : invalidExcludes) {
			message.append("\t- ").append(exclude).append(String.format("%n"));
		}
		throw new IllegalStateException(String.format(
				"The following classes could not be excluded because they are not auto-configuration classes:%n%s",
				message));
	}

	/**
	 * Return any exclusions that limit the candidate configurations.
	 * @param metadata the source metadata
	 * @param attributes the {@link #getAttributes(AnnotationMetadata) annotation
	 * attributes}
	 * @return exclusions or an empty set
	 */
	protected Set<String> getExclusions(AnnotationMetadata metadata, AnnotationAttributes attributes) {
		Set<String> excluded = new LinkedHashSet<>();
		excluded.addAll(asList(attributes, "exclude"));
		excluded.addAll(asList(attributes, "excludeName"));
		excluded.addAll(getExcludeAutoConfigurationsProperty());
		return excluded;
	}

	/**
	 * Returns the auto-configurations excluded by the
	 * {@code spring.autoconfigure.exclude} property.
	 * @return excluded auto-configurations
	 * @since 2.3.2
	 */
	protected List<String> getExcludeAutoConfigurationsProperty() {
		Environment environment = getEnvironment();
		if (environment == null) {
			return Collections.emptyList();
		}
		if (environment instanceof ConfigurableEnvironment) {
			Binder binder = Binder.get(environment);
			return binder.bind(PROPERTY_NAME_AUTOCONFIGURE_EXCLUDE, String[].class)
				.map(Arrays::asList)
				.orElse(Collections.emptyList());
		}
		String[] excludes = environment.getProperty(PROPERTY_NAME_AUTOCONFIGURE_EXCLUDE, String[].class);
		return (excludes != null) ? Arrays.asList(excludes) : Collections.emptyList();
	}

	/**
	 * Retrieves the list of auto-configuration import filters.
	 * @return the list of auto-configuration import filters
	 */
	protected List<AutoConfigurationImportFilter> getAutoConfigurationImportFilters() {
		return SpringFactoriesLoader.loadFactories(AutoConfigurationImportFilter.class, this.beanClassLoader);
	}

	/**
	 * Returns the configuration class filter.
	 * @return the configuration class filter
	 */
	private ConfigurationClassFilter getConfigurationClassFilter() {
		if (this.configurationClassFilter == null) {
			List<AutoConfigurationImportFilter> filters = getAutoConfigurationImportFilters();
			for (AutoConfigurationImportFilter filter : filters) {
				invokeAwareMethods(filter);
			}
			this.configurationClassFilter = new ConfigurationClassFilter(this.beanClassLoader, filters);
		}
		return this.configurationClassFilter;
	}

	/**
	 * Removes duplicate elements from the given list.
	 * @param <T> the type of elements in the list
	 * @param list the list from which duplicate elements are to be removed
	 * @return a new list containing the unique elements from the original list, in the
	 * same order
	 */
	protected final <T> List<T> removeDuplicates(List<T> list) {
		return new ArrayList<>(new LinkedHashSet<>(list));
	}

	/**
	 * Converts the given array of strings to a list of strings.
	 * @param attributes the annotation attributes containing the array of strings
	 * @param name the name of the attribute containing the array of strings
	 * @return a list of strings converted from the given array of strings
	 */
	protected final List<String> asList(AnnotationAttributes attributes, String name) {
		String[] value = attributes.getStringArray(name);
		return Arrays.asList(value);
	}

	/**
	 * Fires the AutoConfigurationImportEvents to the registered
	 * AutoConfigurationImportListeners.
	 * @param configurations the list of configurations to be imported
	 * @param exclusions the set of configurations to be excluded
	 */
	private void fireAutoConfigurationImportEvents(List<String> configurations, Set<String> exclusions) {
		List<AutoConfigurationImportListener> listeners = getAutoConfigurationImportListeners();
		if (!listeners.isEmpty()) {
			AutoConfigurationImportEvent event = new AutoConfigurationImportEvent(this, configurations, exclusions);
			for (AutoConfigurationImportListener listener : listeners) {
				invokeAwareMethods(listener);
				listener.onAutoConfigurationImportEvent(event);
			}
		}
	}

	/**
	 * Retrieves the list of AutoConfigurationImportListeners.
	 * @return the list of AutoConfigurationImportListeners
	 */
	protected List<AutoConfigurationImportListener> getAutoConfigurationImportListeners() {
		return SpringFactoriesLoader.loadFactories(AutoConfigurationImportListener.class, this.beanClassLoader);
	}

	/**
	 * Invokes the aware methods on the given instance.
	 * @param instance the object instance
	 */
	private void invokeAwareMethods(Object instance) {
		if (instance instanceof Aware) {
			if (instance instanceof BeanClassLoaderAware beanClassLoaderAwareInstance) {
				beanClassLoaderAwareInstance.setBeanClassLoader(this.beanClassLoader);
			}
			if (instance instanceof BeanFactoryAware beanFactoryAwareInstance) {
				beanFactoryAwareInstance.setBeanFactory(this.beanFactory);
			}
			if (instance instanceof EnvironmentAware environmentAwareInstance) {
				environmentAwareInstance.setEnvironment(this.environment);
			}
			if (instance instanceof ResourceLoaderAware resourceLoaderAwareInstance) {
				resourceLoaderAwareInstance.setResourceLoader(this.resourceLoader);
			}
		}
	}

	/**
	 * Set the BeanFactory that this object runs in.
	 * <p>
	 * Invoked after population of normal bean properties but before an init callback such
	 * as InitializingBean's {@code afterPropertiesSet} or a custom init-method. Invoked
	 * after ResourceLoaderAware's {@code setResourceLoader},
	 * ApplicationEventPublisherAware's {@code setApplicationEventPublisher} and
	 * MessageSourceAware's {@code setMessageSource}.
	 * <p>
	 * This method allows the bean instance to perform initialization based on its bean
	 * factory context, such as setting up custom editors or registering beans.
	 * <p>
	 * This implementation checks that the bean factory is an instance of
	 * {@code ConfigurableListableBeanFactory} and assigns it to the {@code beanFactory}
	 * field.
	 * @param beanFactory the BeanFactory that this object runs in
	 * @throws BeansException if initialization failed
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet
	 * @see org.springframework.context.ResourceLoaderAware#setResourceLoader
	 * @see org.springframework.context.ApplicationEventPublisherAware#setApplicationEventPublisher
	 * @see org.springframework.context.MessageSourceAware#setMessageSource
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		Assert.isInstanceOf(ConfigurableListableBeanFactory.class, beanFactory);
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
	}

	/**
	 * Returns the ConfigurableListableBeanFactory associated with this
	 * AutoConfigurationImportSelector.
	 * @return the ConfigurableListableBeanFactory associated with this
	 * AutoConfigurationImportSelector
	 */
	protected final ConfigurableListableBeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	/**
	 * Set the ClassLoader to be used for loading bean classes.
	 * @param classLoader the ClassLoader to be used
	 */
	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	/**
	 * Returns the ClassLoader used for loading the beans.
	 * @return the ClassLoader used for loading the beans
	 */
	protected ClassLoader getBeanClassLoader() {
		return this.beanClassLoader;
	}

	/**
	 * Set the environment for this AutoConfigurationImportSelector.
	 * @param environment the environment to set
	 */
	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	/**
	 * Returns the environment associated with this AutoConfigurationImportSelector.
	 * @return the environment associated with this AutoConfigurationImportSelector
	 */
	protected final Environment getEnvironment() {
		return this.environment;
	}

	/**
	 * Set the resource loader to be used for loading resources.
	 * @param resourceLoader the resource loader to be set
	 */
	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	/**
	 * Returns the resource loader used by this AutoConfigurationImportSelector.
	 * @return the resource loader used by this AutoConfigurationImportSelector
	 */
	protected final ResourceLoader getResourceLoader() {
		return this.resourceLoader;
	}

	/**
	 * Returns the order in which this AutoConfigurationImportSelector should be
	 * processed. The order is determined by subtracting 1 from the LOWEST_PRECEDENCE
	 * constant defined in the Ordered interface.
	 * @return the order of this AutoConfigurationImportSelector
	 */
	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE - 1;
	}

	/**
	 * ConfigurationClassFilter class.
	 */
	private static class ConfigurationClassFilter {

		private final AutoConfigurationMetadata autoConfigurationMetadata;

		private final List<AutoConfigurationImportFilter> filters;

		/**
		 * Constructs a new ConfigurationClassFilter with the specified class loader and
		 * list of auto configuration import filters.
		 * @param classLoader the class loader to use for loading auto configuration
		 * metadata
		 * @param filters the list of auto configuration import filters to apply
		 */
		ConfigurationClassFilter(ClassLoader classLoader, List<AutoConfigurationImportFilter> filters) {
			this.autoConfigurationMetadata = AutoConfigurationMetadataLoader.loadMetadata(classLoader);
			this.filters = filters;
		}

		/**
		 * Filters the given list of configurations based on the registered filters.
		 * @param configurations the list of configurations to be filtered
		 * @return the filtered list of configurations
		 */
		List<String> filter(List<String> configurations) {
			long startTime = System.nanoTime();
			String[] candidates = StringUtils.toStringArray(configurations);
			boolean skipped = false;
			for (AutoConfigurationImportFilter filter : this.filters) {
				boolean[] match = filter.match(candidates, this.autoConfigurationMetadata);
				for (int i = 0; i < match.length; i++) {
					if (!match[i]) {
						candidates[i] = null;
						skipped = true;
					}
				}
			}
			if (!skipped) {
				return configurations;
			}
			List<String> result = new ArrayList<>(candidates.length);
			for (String candidate : candidates) {
				if (candidate != null) {
					result.add(candidate);
				}
			}
			if (logger.isTraceEnabled()) {
				int numberFiltered = configurations.size() - result.size();
				logger.trace("Filtered " + numberFiltered + " auto configuration class in "
						+ TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime) + " ms");
			}
			return result;
		}

	}

	/**
	 * AutoConfigurationGroup class.
	 */
	private static final class AutoConfigurationGroup
			implements DeferredImportSelector.Group, BeanClassLoaderAware, BeanFactoryAware, ResourceLoaderAware {

		private final Map<String, AnnotationMetadata> entries = new LinkedHashMap<>();

		private final List<AutoConfigurationEntry> autoConfigurationEntries = new ArrayList<>();

		private ClassLoader beanClassLoader;

		private BeanFactory beanFactory;

		private ResourceLoader resourceLoader;

		private AutoConfigurationMetadata autoConfigurationMetadata;

		/**
		 * Sets the class loader to be used for loading beans in this
		 * AutoConfigurationGroup.
		 * @param classLoader the class loader to be set
		 */
		@Override
		public void setBeanClassLoader(ClassLoader classLoader) {
			this.beanClassLoader = classLoader;
		}

		/**
		 * Sets the bean factory for this AutoConfigurationGroup.
		 * @param beanFactory the bean factory to be set
		 */
		@Override
		public void setBeanFactory(BeanFactory beanFactory) {
			this.beanFactory = beanFactory;
		}

		/**
		 * Sets the resource loader for this AutoConfigurationGroup.
		 * @param resourceLoader the resource loader to be set
		 */
		@Override
		public void setResourceLoader(ResourceLoader resourceLoader) {
			this.resourceLoader = resourceLoader;
		}

		/**
		 * Process the annotation metadata and deferred import selector.
		 * @param annotationMetadata the annotation metadata
		 * @param deferredImportSelector the deferred import selector
		 * @throws IllegalArgumentException if the deferred import selector is not an
		 * instance of AutoConfigurationImportSelector
		 */
		@Override
		public void process(AnnotationMetadata annotationMetadata, DeferredImportSelector deferredImportSelector) {
			Assert.state(deferredImportSelector instanceof AutoConfigurationImportSelector,
					() -> String.format("Only %s implementations are supported, got %s",
							AutoConfigurationImportSelector.class.getSimpleName(),
							deferredImportSelector.getClass().getName()));
			AutoConfigurationEntry autoConfigurationEntry = ((AutoConfigurationImportSelector) deferredImportSelector)
				.getAutoConfigurationEntry(annotationMetadata);
			this.autoConfigurationEntries.add(autoConfigurationEntry);
			for (String importClassName : autoConfigurationEntry.getConfigurations()) {
				this.entries.putIfAbsent(importClassName, annotationMetadata);
			}
		}

		/**
		 * Returns an iterable of entries representing the selected imports for
		 * auto-configuration. If the autoConfigurationEntries list is empty, an empty
		 * list is returned.
		 *
		 * The method first collects all exclusions from the autoConfigurationEntries list
		 * and stores them in a set. Then, it collects all configurations from the
		 * autoConfigurationEntries list and stores them in a linked hash set. The method
		 * removes all exclusions from the set of configurations.
		 *
		 * The method then sorts the remaining configurations using the
		 * sortAutoConfigurations method and the autoConfigurationMetadata. Each sorted
		 * configuration is mapped to an Entry object, which contains the entry and the
		 * import class name. The mapped entries are collected into a list and returned.
		 * @return an iterable of entries representing the selected imports for
		 * auto-configuration
		 */
		@Override
		public Iterable<Entry> selectImports() {
			if (this.autoConfigurationEntries.isEmpty()) {
				return Collections.emptyList();
			}
			Set<String> allExclusions = this.autoConfigurationEntries.stream()
				.map(AutoConfigurationEntry::getExclusions)
				.flatMap(Collection::stream)
				.collect(Collectors.toSet());
			Set<String> processedConfigurations = this.autoConfigurationEntries.stream()
				.map(AutoConfigurationEntry::getConfigurations)
				.flatMap(Collection::stream)
				.collect(Collectors.toCollection(LinkedHashSet::new));
			processedConfigurations.removeAll(allExclusions);

			return sortAutoConfigurations(processedConfigurations, getAutoConfigurationMetadata()).stream()
				.map((importClassName) -> new Entry(this.entries.get(importClassName), importClassName))
				.toList();
		}

		/**
		 * Retrieves the auto-configuration metadata.
		 * @return The auto-configuration metadata.
		 */
		private AutoConfigurationMetadata getAutoConfigurationMetadata() {
			if (this.autoConfigurationMetadata == null) {
				this.autoConfigurationMetadata = AutoConfigurationMetadataLoader.loadMetadata(this.beanClassLoader);
			}
			return this.autoConfigurationMetadata;
		}

		/**
		 * Sorts the given set of auto configurations based on their priority order.
		 * @param configurations the set of auto configurations to be sorted
		 * @param autoConfigurationMetadata the metadata containing information about the
		 * auto configurations
		 * @return a sorted list of auto configurations
		 */
		private List<String> sortAutoConfigurations(Set<String> configurations,
				AutoConfigurationMetadata autoConfigurationMetadata) {
			return new AutoConfigurationSorter(getMetadataReaderFactory(), autoConfigurationMetadata)
				.getInPriorityOrder(configurations);
		}

		/**
		 * Returns the MetadataReaderFactory used by this AutoConfigurationGroup. If the
		 * SharedMetadataReaderFactoryContextInitializer bean is available in the bean
		 * factory, it will be used. Otherwise, a new instance of
		 * CachingMetadataReaderFactory will be created using the provided resource
		 * loader.
		 * @return the MetadataReaderFactory used by this AutoConfigurationGroup
		 */
		private MetadataReaderFactory getMetadataReaderFactory() {
			try {
				return this.beanFactory.getBean(SharedMetadataReaderFactoryContextInitializer.BEAN_NAME,
						MetadataReaderFactory.class);
			}
			catch (NoSuchBeanDefinitionException ex) {
				return new CachingMetadataReaderFactory(this.resourceLoader);
			}
		}

	}

	/**
	 * AutoConfigurationEntry class.
	 */
	protected static class AutoConfigurationEntry {

		private final List<String> configurations;

		private final Set<String> exclusions;

		/**
		 * Constructs a new AutoConfigurationEntry with an empty list of configurations
		 * and an empty set of exclusions.
		 */
		private AutoConfigurationEntry() {
			this.configurations = Collections.emptyList();
			this.exclusions = Collections.emptySet();
		}

		/**
		 * Create an entry with the configurations that were contributed and their
		 * exclusions.
		 * @param configurations the configurations that should be imported
		 * @param exclusions the exclusions that were applied to the original list
		 */
		AutoConfigurationEntry(Collection<String> configurations, Collection<String> exclusions) {
			this.configurations = new ArrayList<>(configurations);
			this.exclusions = new HashSet<>(exclusions);
		}

		/**
		 * Returns the list of configurations.
		 * @return the list of configurations
		 */
		public List<String> getConfigurations() {
			return this.configurations;
		}

		/**
		 * Returns the set of exclusions.
		 * @return the set of exclusions
		 */
		public Set<String> getExclusions() {
			return this.exclusions;
		}

	}

}
