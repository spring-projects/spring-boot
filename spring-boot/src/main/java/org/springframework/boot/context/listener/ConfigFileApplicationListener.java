/*
 * Copyright 2010-2014 the original author or authors.
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

package org.springframework.boot.context.listener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.bind.PropertySourcesPropertyValues;
import org.springframework.boot.bind.RelaxedDataBinder;
import org.springframework.boot.config.DefaultPropertySourceLoadersFactory;
import org.springframework.boot.config.PropertySourceLoader;
import org.springframework.boot.config.PropertySourceLoadersFactory;
import org.springframework.boot.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ApplicationListener} that configures the context environment by loading
 * properties from well known file locations. By default properties will be loaded from
 * 'application.properties' and/or 'application.yml' files in the following locations:
 * <ul>
 * <li>classpath:</li>
 * <li>file:./</li>
 * <li>classpath:config/</li>
 * <li>file:./config/:</li>
 * </ul>
 * <p>
 * Alternative locations and names can be specified using
 * {@link #setSearchLocations(String[])} and {@link #setNames(String)}.
 * <p>
 * Additional files will also be loaded based on active profiles. For example if a 'web'
 * profile is active 'application-web.properties' and 'application-web.yml' will be
 * considered.
 * <p>
 * The 'spring.config.name' property can be used to specify an alternative name to load or
 * alternatively the 'spring.config.location' property can be used to specify an exact
 * resource location.
 * 
 * @author Dave Syer
 * @author Phillip Webb
 */
public class ConfigFileApplicationListener implements
		ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

	private static final String ACTIVE_PROFILES_PROPERTY = "spring.profiles.active";

	private static final String LOCATION_VARIABLE = "${spring.config.location}";

	private String[] searchLocations = new String[] { "classpath:/", "file:./",
			"classpath:/config/", "file:./config/" };

	private String names = "${spring.config.name:application}";

	private int order = Integer.MIN_VALUE + 10;

	private final ConversionService conversionService = new DefaultConversionService();

	private final Map<String, PropertySource<?>> cache = new HashMap<String, PropertySource<?>>();

	private final PropertySourceAnnotations annotations = new PropertySourceAnnotations();

	private PropertySourceLoadersFactory propertySourceLoadersFactory = new DefaultPropertySourceLoadersFactory();

	/**
	 * Binds the early {@link Environment} to the {@link SpringApplication}. This makes it
	 * possible to set {@link SpringApplication} properties dynamically, like the sources
	 * ("spring.main.sources" - a CSV list) the flag to indicate a web environment
	 * ("spring.main.web_environment=true") or the flag to switch off the banner
	 * ("spring.main.show_banner=false").
	 */
	@Override
	public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
		Environment environment = event.getEnvironment();
		if (environment instanceof ConfigurableEnvironment) {
			configure((ConfigurableEnvironment) environment, event.getSpringApplication());
		}
	}

	private void configure(ConfigurableEnvironment environment,
			SpringApplication springApplication) {
		for (Object source : springApplication.getSources()) {
			this.annotations.addFromSource(source);
		}
		load(environment, new DefaultResourceLoader());
		environment.getPropertySources().addAfter(
				StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
				new RandomValuePropertySource("random"));

		int sourcesSizeBefore = springApplication.getSources().size();
		// Set bean properties from the early environment
		PropertyValues propertyValues = new PropertySourcesPropertyValues(
				environment.getPropertySources());
		RelaxedDataBinder binder = new RelaxedDataBinder(springApplication, "spring.main");
		binder.setConversionService(this.conversionService);
		binder.bind(propertyValues);

		if (springApplication.getSources().size() > sourcesSizeBefore) {
			// Configure again in case there are new @PropertySources
			configure(environment, springApplication);
		}
	}

	private void load(ConfigurableEnvironment environment, ResourceLoader resourceLoader) {

		LoadCandidates candidates = new LoadCandidates(environment, resourceLoader);
		PropertySource<?> defaultProperties = environment.getPropertySources().remove(
				"defaultProperties");

		addActiveProfiles(environment);
		String firstPropertySourceName = loadInitial(environment, resourceLoader,
				candidates);

		// Repeatedly load property sources in case additional profiles are activated
		int numberOfPropertySources;
		do {
			numberOfPropertySources = environment.getPropertySources().size();
			addActiveProfiles(environment);
			loadAgain(environment, resourceLoader, candidates, firstPropertySourceName);
		}
		while (environment.getPropertySources().size() > numberOfPropertySources);

		if (defaultProperties != null) {
			environment.getPropertySources().addLast(defaultProperties);
		}
	}

	/**
	 * @param environment
	 */
	private void addActiveProfiles(ConfigurableEnvironment environment) {
		for (PropertySource<?> propertySource : environment.getPropertySources()) {
			if (propertySource.containsProperty(ACTIVE_PROFILES_PROPERTY)) {
				Object profiles = propertySource.getProperty(ACTIVE_PROFILES_PROPERTY);
				for (String profile : StringUtils.commaDelimitedListToSet(profiles
						.toString())) {
					environment.addActiveProfile(profile);
				}
			}
		}
	}

	private String loadInitial(ConfigurableEnvironment environment,
			ResourceLoader resourceLoader, LoadCandidates candidates) {
		String firstSourceName = null;
		// Initial load allows profiles to be activated
		for (String candidate : candidates) {
			for (String path : StringUtils.commaDelimitedListToStringArray(environment
					.resolvePlaceholders(candidate))) {

				if (LOCATION_VARIABLE.equals(candidate) && !path.contains("$")) {
					if (!path.contains(":")) {
						path = "file:" + path;
					}
					path = StringUtils.cleanPath(path);
				}

				PropertySource<?> source = loadPropertySource(environment,
						resourceLoader, path, null);
				if (source != null) {
					if (firstSourceName == null) {
						firstSourceName = source.getName();
					}
					environment.getPropertySources().addLast(source);
				}
			}
		}
		return firstSourceName;
	}

	private void loadAgain(ConfigurableEnvironment environment,
			ResourceLoader resourceLoader, LoadCandidates candidates,
			String firstPropertySourceName) {
		for (String profile : environment.getActiveProfiles()) {
			for (String candidate : candidates) {
				PropertySource<?> source = loadPropertySource(environment,
						resourceLoader, candidate, profile);
				addBeforeOrLast(environment, firstPropertySourceName, source);
			}
		}
	}

	private void addBeforeOrLast(ConfigurableEnvironment environment,
			String relativePropertySourceName, PropertySource<?> source) {
		if (source != null) {
			MutablePropertySources propertySources = environment.getPropertySources();
			// Originals go at the end so they don't override the specific profiles
			if (relativePropertySourceName != null) {
				propertySources.addBefore(relativePropertySourceName, source);
			}
			else {
				propertySources.addLast(source);
			}
		}
	}

	private PropertySource<?> loadPropertySource(ConfigurableEnvironment environment,
			ResourceLoader resourceLoader, String location, String profile) {

		Class<?> type = this.annotations.configuration(location);

		String suffix = "." + StringUtils.getFilenameExtension(location);
		if (StringUtils.hasLength(profile)) {
			location = location.replace(suffix, "-" + profile + suffix);
		}

		if (isPropertySourceAnnotationOnExcludedType(environment, profile, type, location)) {
			return null;
		}

		Resource resource = resourceLoader.getResource(location);
		String name = this.annotations.name(location);
		name = (name != null ? name : location);
		return getPropertySource(environment, name, resource, profile);
	}

	private boolean isPropertySourceAnnotationOnExcludedType(Environment environment,
			String profile, Class<?> type, String location) {

		if (type == null) {
			// No configuration class to worry about, just a vanilla properties location
			return false;
		}

		if (StringUtils.hasText(profile)
				&& !this.annotations.getLocations().contains(location)) {
			// We are looking for profile specific properties and this one isn't
			// explicitly asked for in propertySourceAnnotations
			return true;
		}

		AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(
				new DefaultListableBeanFactory(), environment);
		int before = reader.getRegistry().getBeanDefinitionCount();
		reader.register(type);
		int after = reader.getRegistry().getBeanDefinitionCount();

		// Return if the configuration class was @Conditional and excluded
		return (after == before);
	}

	private PropertySource<?> getPropertySource(Environment environment, String name,
			Resource resource, String profile) {
		if (resource == null || !resource.exists()) {
			return null;
		}
		String key = resource.getDescription() + (profile == null ? "" : "#" + profile);
		if (this.cache.containsKey(key)) {
			return this.cache.get(key);
		}
		for (PropertySourceLoader loader : this.propertySourceLoadersFactory
				.getLoaders(environment)) {
			if (loader.supports(resource)) {
				PropertySource<?> propertySource = loader.load(name, resource);
				this.cache.put(key, propertySource);
				return propertySource;
			}
		}
		throw new IllegalStateException("No supported loader found for "
				+ "configuration resource: " + resource);
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * Sets the names of the files that should be loaded (excluding file extension) as a
	 * comma separated list. Defaults to "application".
	 */
	public void setNames(String names) {
		this.names = names;
	}

	/**
	 * Set the search locations that will be considered.
	 */
	public void setSearchLocations(String[] searchLocations) {
		this.searchLocations = (searchLocations == null ? null : searchLocations.clone());
	}

	/**
	 * Set the {@link PropertySourceLoadersFactory} that will be used to create
	 * {@link PropertySourceLoader}s.
	 */
	public void setPropertySourceLoadersFactory(
			PropertySourceLoadersFactory propertySourceLoaderFactory) {
		this.propertySourceLoadersFactory = propertySourceLoaderFactory;
	}

	/**
	 * Provides {@link Iterable} access to candidate property sources.
	 */
	private class LoadCandidates implements Iterable<String> {

		private final List<String> candidates;

		public LoadCandidates(ConfigurableEnvironment environment,
				ResourceLoader resourceLoader) {
			Set<String> candidates = new LinkedHashSet<String>();
			addLoadCandidatesFromSearchLocations(environment, candidates);
			candidates.add(LOCATION_VARIABLE);
			// @PropertySource annotation locations go last here (eventually highest
			// priority). This unfortunately isn't the same semantics as @PropertySource
			// in
			// Spring and it's hard to change that (so the property source gets added
			// again in
			// last position by Spring later in the cycle).
			addLoadCandidatesFromAnnotations(resourceLoader, candidates);
			this.candidates = new ArrayList<String>(candidates);
			Collections.reverse(this.candidates);
		}

		private void addLoadCandidatesFromSearchLocations(
				ConfigurableEnvironment environment, Set<String> candidates) {
			for (String location : ConfigFileApplicationListener.this.searchLocations) {
				for (String extension : new String[] { ".properties", ".yml" }) {
					for (String name : StringUtils
							.commaDelimitedListToStringArray(environment
									.resolvePlaceholders(ConfigFileApplicationListener.this.names))) {
						candidates.add(location + name + extension);
					}
				}
			}
		}

		private void addLoadCandidatesFromAnnotations(ResourceLoader resourceLoader,
				Set<String> candidates) {
			for (String location : ConfigFileApplicationListener.this.annotations
					.getLocations()) {
				Resource resource = resourceLoader.getResource(location);
				if (!ConfigFileApplicationListener.this.annotations
						.ignoreResourceNotFound(location) && !resource.exists()) {
					throw new IllegalStateException("Resource not found: " + location);
				}
				candidates.add(location);
			}
		}

		@Override
		public Iterator<String> iterator() {
			return this.candidates.iterator();
		}

	}

	/**
	 * {@link PropertySource} that returns a random value for any property that starts
	 * with {@literal "random."}. Return a {@code byte[]} unless the property name ends
	 * with {@literal ".int} or {@literal ".long"}.
	 */
	private static class RandomValuePropertySource extends PropertySource<Random> {

		public RandomValuePropertySource(String name) {
			super(name, new Random());
		}

		@Override
		public Object getProperty(String name) {
			if (!name.startsWith("random.")) {
				return null;
			}
			if (name.endsWith("int")) {
				return getSource().nextInt();
			}
			if (name.endsWith("long")) {
				return getSource().nextLong();
			}
			byte[] bytes = new byte[32];
			getSource().nextBytes(bytes);
			return DigestUtils.md5DigestAsHex(bytes);
		}

	}

	/**
	 * Holds details collected from
	 * {@link org.springframework.context.annotation.PropertySource} annotations.
	 */
	private static class PropertySourceAnnotations {

		private final Collection<String> locations = new LinkedHashSet<String>();

		private final Map<String, String> names = new HashMap<String, String>();

		private final Map<String, Class<?>> configs = new HashMap<String, Class<?>>();

		private final Map<String, Boolean> ignores = new HashMap<String, Boolean>();

		public void addFromSource(Object source) {
			if (source instanceof Class<?>) {
				addFromSource((Class<?>) source);
			}
		}

		private void addFromSource(Class<?> source) {
			for (org.springframework.context.annotation.PropertySource propertySource : AnnotationUtils
					.getRepeatableAnnotation(source, PropertySources.class,
							org.springframework.context.annotation.PropertySource.class)) {
				add(source, propertySource);
			}
		}

		private void add(Class<?> source,
				org.springframework.context.annotation.PropertySource annotation) {
			this.locations.addAll(Arrays.asList(annotation.value()));
			if (StringUtils.hasText(annotation.name())) {
				for (String location : annotation.value()) {
					this.names.put(location, annotation.name());
				}
			}
			for (String location : annotation.value()) {
				boolean reallyIgnore = annotation.ignoreResourceNotFound();
				if (this.ignores.containsKey(location)) {
					// Only if they all ignore this location will it be ignored
					reallyIgnore &= this.ignores.get(location);
				}
				this.ignores.put(location, reallyIgnore);
				this.configs.put(location, source);
			}
		}

		public Class<?> configuration(String location) {
			return this.configs.get(location);
		}

		public boolean ignoreResourceNotFound(String location) {
			return Boolean.TRUE.equals(this.ignores.get(location));
		}

		public String name(String location) {
			String name = this.names.get(location);
			if (name == null || Collections.frequency(this.names.values(), name) > 1) {
				return null;
			}
			// Only if there is a unique name for this location
			return "boot." + name;
		}

		public Collection<String> getLocations() {
			return this.locations;
		}
	}

}
