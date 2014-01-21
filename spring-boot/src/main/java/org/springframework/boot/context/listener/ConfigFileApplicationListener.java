/*
 * Copyright 2010-2012 the original author or authors.
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationEnvironmentAvailableEvent;
import org.springframework.boot.bind.PropertySourcesPropertyValues;
import org.springframework.boot.bind.RelaxedDataBinder;
import org.springframework.boot.config.PropertiesPropertySourceLoader;
import org.springframework.boot.config.PropertySourceLoader;
import org.springframework.boot.config.YamlPropertySourceLoader;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ApplicationContextInitializer} that configures the context environment by
 * loading properties from well known file locations. By default properties will be loaded
 * from 'application.properties' and/or 'application.yml' files in the following
 * locations:
 * <ul>
 * <li>classpath:</li>
 * <li>file:./</li>
 * <li>classpath:config/</li>
 * <li>file:./config/:</li>
 * </ul>
 * 
 * <p>
 * Alternative locations and names can be specified using
 * {@link #setSearchLocations(String[])} and {@link #setNames(String)}.
 * 
 * <p>
 * Additional files will also be loaded based on active profiles. For example if a 'web'
 * profile is active 'application-web.properties' and 'application-web.yml' will be
 * considered.
 * 
 * <p>
 * The 'spring.config.name' property can be used to specify an alternative name to load or
 * alternatively the 'spring.config.location' property can be used to specify an exact
 * resource location.
 * 
 * @author Dave Syer
 * @author Phillip Webb
 */
public class ConfigFileApplicationListener implements
		ApplicationListener<SpringApplicationEnvironmentAvailableEvent>, Ordered {

	private static final String LOCATION_VARIABLE = "${spring.config.location}";

	private String[] searchLocations = new String[] { "classpath:", "file:./",
			"classpath:config/", "file:./config/" };

	private String names = "${spring.config.name:application}";

	private int order = Integer.MIN_VALUE + 10;

	private Map<String, PropertySource<?>> cached = new HashMap<String, PropertySource<?>>();

	private ConversionService conversionService = new DefaultConversionService();

	private PropertySourceAnnotations propertySourceAnnotations = new PropertySourceAnnotations();

	private PropertySourceLoaderFactory propertySourceLoaderFactory = new DefaultPropertySourceLoaderFactory();

	/**
	 * Binds the early {@link Environment} to the {@link SpringApplication}. This makes it
	 * possible to set {@link SpringApplication} properties dynamically, like the sources
	 * ("spring.main.sources" - a CSV list) the flag to indicate a web environment
	 * ("spring.main.web_environment=true") or the flag to switch off the banner
	 * ("spring.main.show_banner=false").
	 */
	@Override
	public void onApplicationEvent(SpringApplicationEnvironmentAvailableEvent event) {
		Environment created = event.getEnvironment();
		if (created instanceof ConfigurableEnvironment) {
			SpringApplication springApplication = event.getSpringApplication();
			extractPropertySources(springApplication.getSources());
			ConfigurableEnvironment environment = (ConfigurableEnvironment) created;
			load(environment, new DefaultResourceLoader());
			environment.getPropertySources().addAfter(
					StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
					new RandomValuePropertySource("random"));
			int before = springApplication.getSources().size();
			// Set bean properties from the early environment
			PropertyValues propertyValues = new PropertySourcesPropertyValues(
					environment.getPropertySources());
			RelaxedDataBinder binder = new RelaxedDataBinder(springApplication,
					"spring.main");
			binder.setConversionService(this.conversionService);
			binder.bind(propertyValues);
			int after = springApplication.getSources().size();
			if (after > before) {
				// Do it again in case there are new @PropertySources
				onApplicationEvent(event);
			}
		}
	}

	private void extractPropertySources(Set<Object> sources) {
		for (Object source : sources) {
			if (source instanceof Class) {
				Class<?> type = (Class<?>) source;
				for (AnnotationAttributes propertySource : attributesForRepeatable(
						new StandardAnnotationMetadata(type), PropertySources.class,
						org.springframework.context.annotation.PropertySource.class)) {
					this.propertySourceAnnotations.add(type,
							propertySource.getStringArray("value"),
							propertySource.getBoolean("ignoreResourceNotFound"),
							propertySource.getString("name"));
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	static Set<AnnotationAttributes> attributesForRepeatable(AnnotationMetadata metadata,
			Class<?> containerClass, Class<?> annotationClass) {
		Set<AnnotationAttributes> result = new LinkedHashSet<AnnotationAttributes>();

		addAttributesIfNotNull(result,
				metadata.getAnnotationAttributes(annotationClass.getName(), false));

		Map<String, Object> container = metadata.getAnnotationAttributes(
				containerClass.getName(), false);
		if (container != null && container.containsKey("value")) {
			for (Map<String, Object> containedAttributes : (Map<String, Object>[]) container
					.get("value")) {
				addAttributesIfNotNull(result, containedAttributes);
			}
		}
		return Collections.unmodifiableSet(result);
	}

	private static void addAttributesIfNotNull(Set<AnnotationAttributes> result,
			Map<String, Object> attributes) {
		if (attributes != null) {
			result.add(AnnotationAttributes.fromMap(attributes));
		}
	}

	private void load(ConfigurableEnvironment environment, ResourceLoader resourceLoader) {

		List<String> candidates = getCandidateLocations(resourceLoader);
		Collections.reverse(candidates);
		PropertySource<?> removed = environment.getPropertySources().remove(
				"defaultProperties");

		String first = null;
		// Initial load allows profiles to be activated
		for (String candidate : candidates) {
			PropertySource<?> source = load(environment, resourceLoader, candidate, null);
			if (source != null) {
				if (first == null) {
					first = source.getName();
				}
				environment.getPropertySources().addLast(source);
			}
		}

		if (environment.containsProperty("spring.profiles.active")) {
			Set<String> profiles = StringUtils.commaDelimitedListToSet(environment
					.getProperty("spring.profiles.active").toString());
			for (String active : profiles) {
				// allow document with no profile to set the active one
				environment.addActiveProfile(active);
			}

		}

		// Second load for specific profiles
		for (String profile : environment.getActiveProfiles()) {
			for (String candidate : candidates) {
				PropertySource<?> source = load(environment, resourceLoader, candidate,
						profile);
				if (source != null) {
					if (first != null) {
						// Originals go at the end so they don't override the specific
						// profiles
						environment.getPropertySources().addBefore(first, source);
					}
					else {
						environment.getPropertySources().addLast(source);
					}
				}
			}
		}

		if (removed != null) {
			environment.getPropertySources().addLast(removed);
		}
	}

	private List<String> getCandidateLocations(ResourceLoader resourceLoader) {
		Set<String> candidates = new LinkedHashSet<String>();
		for (String searchLocation : this.searchLocations) {
			for (String extension : new String[] { ".properties", ".yml" }) {
				for (String name : StringUtils
						.commaDelimitedListToStringArray(this.names)) {
					String location = searchLocation + name + extension;
					candidates.add(location);
				}
			}
		}
		candidates.add(LOCATION_VARIABLE);
		/*
		 * @PropertySource annotation locations go last here (eventually highest
		 * priority). This unfortunately isn't the same semantics as @PropertySource in
		 * Spring and it's hard to change that (so the property source gets added again in
		 * last position by Spring later in the cycle).
		 */
		for (String location : this.propertySourceAnnotations.locations()) {
			Resource resource = resourceLoader.getResource(location);
			if (!this.propertySourceAnnotations.ignoreResourceNotFound(location)
					&& !resource.exists()) {
				throw new IllegalStateException("Resource not found: " + location);
			}
			candidates.add(location);
		}
		return new ArrayList<String>(candidates);
	}

	private PropertySource<?> load(ConfigurableEnvironment environment,
			ResourceLoader resourceLoader, String location, String profile) {

		String path = environment.resolvePlaceholders(location);
		if (LOCATION_VARIABLE.equals(location) && !path.contains("$")) {
			if (!path.contains(":")) {
				path = "file:" + path;
			}
			path = StringUtils.cleanPath(path);
		}
		location = path;

		String suffix = "." + StringUtils.getFilenameExtension(location);
		Class<?> type = this.propertySourceAnnotations.configuration(location);

		if (StringUtils.hasLength(profile)) {
			location = location.replace(suffix, "-" + profile + suffix);
		}

		if (isPropertySourceAnnotationOnExcludedType(environment, profile, type, location)) {
			return null;
		}

		List<PropertySourceLoader> loaders = this.propertySourceLoaderFactory
				.getLoaders(environment);

		Resource resource = resourceLoader.getResource(location);
		String name = this.propertySourceAnnotations.name(location);
		if (name == null) {
			name = location;
		}
		PropertySource<?> propertySource = getPropertySource(name, resource, profile,
				loaders);
		if (propertySource == null) {
			return null;
		}
		return propertySource;
	}

	private boolean isPropertySourceAnnotationOnExcludedType(Environment environment,
			String profile, Class<?> type, String location) {
		if (type == null) {
			// No configuration class to worry about, just a vanilla properties location
			return false;
		}
		if (StringUtils.hasText(profile)
				&& !this.propertySourceAnnotations.locations().contains(location)) {
			// We are looking for profile specific properties and this one isn't
			// explicitly asked for in propertySourceAnnotations
			return true;
		}
		AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(
				new DefaultListableBeanFactory(), environment);
		int before = reader.getRegistry().getBeanDefinitionCount();
		reader.register(type);
		int after = reader.getRegistry().getBeanDefinitionCount();
		if (after == before) {
			// The configuration class was @Conditional and excluded
			return true;
		}
		return false;
	}

	private PropertySource<?> getPropertySource(String name, Resource resource,
			String profile, List<PropertySourceLoader> loaders) {
		if (resource == null || !resource.exists()) {
			return null;
		}
		String key = resource.getDescription() + (profile == null ? "" : "#" + profile);
		if (this.cached.containsKey(key)) {
			return this.cached.get(key);
		}
		boolean satisfied = true;
		for (PropertySourceLoader loader : loaders) {
			if (loader.supports(resource)) {
				PropertySource<?> propertySource = loader.load(name, resource);
				this.cached.put(key, propertySource);
				return propertySource;
			}
			else {
				satisfied = false;
			}
		}
		if (!satisfied) {
			throw new IllegalStateException(
					"No supported loader found for configuration resource: " + resource);
		}
		return null;
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
	 * @param propertySourceLoaderFactory the factory to set
	 */
	public void setPropertySourceLoaderFactory(
			PropertySourceLoaderFactory propertySourceLoaderFactory) {
		this.propertySourceLoaderFactory = propertySourceLoaderFactory;
	}

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

	private static class PropertySourceAnnotations {

		private Collection<String> locations = new LinkedHashSet<String>();

		private Map<String, String> names = new HashMap<String, String>();

		private Map<String, Class<?>> configs = new HashMap<String, Class<?>>();

		private Map<String, Boolean> ignores = new HashMap<String, Boolean>();

		public void add(Class<?> source, String[] locations,
				boolean ignoreResourceNotFound, String name) {
			this.locations.addAll(Arrays.asList(locations));
			if (StringUtils.hasText(name)) {
				for (String location : locations) {
					this.names.put(location, name);
				}
			}
			for (String location : locations) {
				boolean reallyIgnore = ignoreResourceNotFound;
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
			return this.ignores.containsKey(location) ? this.ignores.get(location)
					: false;
		}

		public String name(String location) {
			String name = this.names.get(location);
			if (name == null || Collections.frequency(this.names.values(), name) > 1) {
				return null;
			}
			// Only if there is a unique name for this location
			return "boot." + name;
		}

		public Collection<String> locations() {
			return this.locations;
		}
	}

	public static interface PropertySourceLoaderFactory {
		List<PropertySourceLoader> getLoaders(Environment environment);
	}

	private static class DefaultPropertySourceLoaderFactory implements
			PropertySourceLoaderFactory {

		@Override
		public List<PropertySourceLoader> getLoaders(Environment environment) {
			ArrayList<PropertySourceLoader> loaders = new ArrayList<PropertySourceLoader>();
			loaders.add(new PropertiesPropertySourceLoader());
			if (ClassUtils.isPresent("org.yaml.snakeyaml.Yaml", null)) {
				loaders.add(YamlPropertySourceLoader.springProfileAwareLoader(environment
						.getActiveProfiles()));
			}
			return loaders;
		}

	}

}
