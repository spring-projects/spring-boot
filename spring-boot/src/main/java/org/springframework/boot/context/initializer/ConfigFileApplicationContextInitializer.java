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

package org.springframework.boot.context.initializer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.springframework.beans.PropertyValues;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationInitializer;
import org.springframework.boot.bind.PropertySourcesPropertyValues;
import org.springframework.boot.bind.RelaxedDataBinder;
import org.springframework.boot.config.PropertiesPropertySourceLoader;
import org.springframework.boot.config.PropertySourceLoader;
import org.springframework.boot.config.YamlPropertySourceLoader;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
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
public class ConfigFileApplicationContextInitializer implements
		ApplicationContextInitializer<ConfigurableApplicationContext>,
		SpringApplicationInitializer, Ordered, EnvironmentAware {

	private static final String LOCATION_VARIABLE = "${spring.config.location}";

	private Environment environment;

	private String[] searchLocations = new String[] { "classpath:", "file:./",
			"classpath:config/", "file:./config/" };

	private String names = "${spring.config.name:application}";

	private int order = Integer.MIN_VALUE + 10;

	private Map<String, PropertySource<?>> cached = new HashMap<String, PropertySource<?>>();

	private ConversionService conversionService = new DefaultConversionService();

	/**
	 * Binds the early {@link Environment} to the {@link SpringApplication}. This makes it
	 * possible to set {@link SpringApplication} properties dynamically, like the sources
	 * ("spring.main.sources" - a CSV list) the flag to indicate a web environment
	 * ("spring.main.web_environment=true") or the flag to switch off the banner
	 * ("spring.main.show_banner=false").
	 */
	@Override
	public void initialize(SpringApplication springApplication, String[] args) {
		if (this.environment instanceof ConfigurableEnvironment) {
			ConfigurableEnvironment environment = (ConfigurableEnvironment) this.environment;
			load(environment, new DefaultResourceLoader());
			environment.getPropertySources().addAfter(
					StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
					new RandomValuePropertySource("random"));
			// Set bean properties from the early environment
			PropertyValues propertyValues = new PropertySourcesPropertyValues(
					environment.getPropertySources());
			RelaxedDataBinder binder = new RelaxedDataBinder(springApplication,
					"spring.main");
			binder.setConversionService(this.conversionService);
			binder.bind(propertyValues);
		}
	}

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		load(applicationContext.getEnvironment(), applicationContext);
	}

	private void load(ConfigurableEnvironment environment, ResourceLoader resourceLoader) {

		List<String> candidates = getCandidateLocations();
		Collections.reverse(candidates);
		PropertySource<?> removed = environment.getPropertySources().remove(
				"defaultProperties");

		List<PropertySource<?>> sources = new ArrayList<PropertySource<?>>();
		// Initial load allows profiles to be activated
		for (String candidate : candidates) {
			PropertySource<?> source = load(environment, resourceLoader, candidate, null);
			if (source != null) {
				sources.add(source);
			}
		}

		// Second load for specific profiles
		for (String profile : environment.getActiveProfiles()) {
			for (String candidate : candidates) {
				PropertySource<?> source = load(environment, resourceLoader, candidate,
						profile);
				if (source != null) {
					environment.getPropertySources().addLast(source);
				}
			}
		}

		// Originals go at the end so they don't override the specific profiles
		for (PropertySource<?> source : sources) {
			environment.getPropertySources().addLast(source);
		}

		if (removed != null) {
			environment.getPropertySources().addLast(removed);
		}
	}

	private List<String> getCandidateLocations() {
		List<String> candidates = new ArrayList<String>();
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
		return candidates;
	}

	private PropertySource<?> load(ConfigurableEnvironment environment,
			ResourceLoader resourceLoader, String location, String profile) {
		location = environment.resolvePlaceholders(location);
		String suffix = "." + StringUtils.getFilenameExtension(location);

		if (StringUtils.hasLength(profile)) {
			location = location.replace(suffix, "-" + profile + suffix);
		}

		List<PropertySourceLoader> loaders = new ArrayList<PropertySourceLoader>();
		loaders.add(new PropertiesPropertySourceLoader());
		if (ClassUtils.isPresent("org.yaml.snakeyaml.Yaml", null)) {
			loaders.add(YamlPropertySourceLoader.springProfileAwareLoader(environment
					.getActiveProfiles()));
		}

		Resource resource = resourceLoader.getResource(location);
		PropertySource<?> propertySource = getPropertySource(resource, profile, loaders);
		if (propertySource == null) {
			return null;
		}
		if (propertySource.containsProperty("spring.profiles.active")) {
			Set<String> profiles = StringUtils.commaDelimitedListToSet(propertySource
					.getProperty("spring.profiles.active").toString());
			for (String active : profiles) {
				// allow document with no profile to set the active one
				environment.addActiveProfile(active);
			}

		}
		return propertySource;
	}

	private PropertySource<?> getPropertySource(Resource resource, String profile,
			List<PropertySourceLoader> loaders) {
		String key = resource.getDescription() + (profile == null ? "" : "#" + profile);
		if (this.cached.containsKey(key)) {
			return this.cached.get(key);
		}
		for (PropertySourceLoader loader : loaders) {
			if (resource != null && resource.exists() && loader.supports(resource)) {
				PropertySource<?> propertySource = loader.load(resource);
				this.cached.put(key, propertySource);
				return propertySource;
			}
		}
		return null;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
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

}
