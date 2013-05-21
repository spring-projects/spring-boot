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
package org.springframework.bootstrap.context.initializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.springframework.bootstrap.config.YamlProcessor.ArrayDocumentMatcher;
import org.springframework.bootstrap.config.YamlProcessor.DocumentMatcher;
import org.springframework.bootstrap.config.YamlProcessor.MatchStatus;
import org.springframework.bootstrap.config.YamlPropertiesFactoryBean;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.Ordered;
import org.springframework.core.env.CommandLinePropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ApplicationContextInitializer} that configures the context environment by
 * loading properties from well known file locations. By default properties will be loaded
 * from 'application.properties' and/or 'application.yaml' files in the following
 * locations:
 * <ul>
 * <li>classpath:</li>
 * <li>file:./</li>
 * <li>classpath:config/</li>
 * <li>file:./config/:</li>
 * </ul>
 * <p>
 * Alternative locations and names can be specified using
 * {@link #setSearchLocations(String[])} and {@link #setName(String)}.
 * 
 * <p>
 * Additional files will also be loaded based on active profiles. For example if a 'web'
 * profile is active 'application-web.properties' and 'application-web.yaml' will be
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
		ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {

	private static final Loader[] LOADERS = { new PropertiesLoader(), new YamlLoader() };

	private static final String LOCATION_VARIABLE = "${spring.config.location}";

	private String[] searchLocations = new String[] { "classpath:", "file:./",
			"classpath:config/", "file:./config/" };

	private String name = "${spring.config.name:application}";

	private int order = Integer.MIN_VALUE;

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		List<String> candidates = getCandidateLocations(applicationContext);

		// Initial load allows profiles to be activated
		for (String candidate : candidates) {
			load(applicationContext, candidate, null);
		}

		// Second load for specific profiles
		for (String profile : applicationContext.getEnvironment().getActiveProfiles()) {
			for (String candidate : candidates) {
				load(applicationContext, candidate, profile);
			}
		}
	}

	private List<String> getCandidateLocations(
			ConfigurableApplicationContext applicationContext) {
		List<String> candidates = new ArrayList<String>();
		for (String searchLocation : this.searchLocations) {
			for (Loader loader : LOADERS) {
				for (String extension : loader.getExtensions()) {
					String location = searchLocation + this.name + extension;
					candidates.add(location);
				}
			}
		}
		candidates.add(LOCATION_VARIABLE);
		return candidates;
	}

	private void load(ConfigurableApplicationContext applicationContext, String location,
			String profile) {
		location = applicationContext.getEnvironment().resolvePlaceholders(location);
		String suffix = "." + StringUtils.getFilenameExtension(location);
		if (StringUtils.hasLength(profile)) {
			location = location.replace(suffix, "-" + profile + suffix);
		}
		for (Loader loader : LOADERS) {
			if (loader.getExtensions().contains(suffix.toLowerCase())) {
				Resource resource = applicationContext.getResource(location);
				if (resource != null && resource.exists()) {
					loader.load(resource, applicationContext);
				}
				return;
			}
		}
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * Sets the name of the file that should be loaded (excluding any file extension).
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Set the search locations that will be considered.
	 */
	public void setSearchLocations(String[] searchLocations) {
		this.searchLocations = searchLocations;
	}

	/**
	 * Strategy interface used to load a {@link PropertySource}.
	 */
	private static interface Loader {

		/**
		 * @return The supported extensions (including '.' and in lowercase)
		 */
		public Set<String> getExtensions();

		/**
		 * Load the resource into the destination application context.
		 */
		void load(Resource resource, ConfigurableApplicationContext applicationContext);

	}

	/**
	 * Strategy to load '.properties' files.
	 */
	private static class PropertiesLoader implements Loader {

		@Override
		public Set<String> getExtensions() {
			return Collections.singleton(".properties");
		}

		@Override
		public void load(Resource resource,
				ConfigurableApplicationContext applicationContext) {
			try {
				Properties properties = loadProperties(resource, applicationContext);
				MutablePropertySources propertySources = applicationContext
						.getEnvironment().getPropertySources();
				if (propertySources
						.contains(CommandLinePropertySource.COMMAND_LINE_PROPERTY_SOURCE_NAME)) {
					propertySources.addAfter(
							CommandLinePropertySource.COMMAND_LINE_PROPERTY_SOURCE_NAME,
							new PropertiesPropertySource(resource.getDescription(),
									properties));

				} else {
					propertySources.addFirst(new PropertiesPropertySource(resource
							.getDescription(), properties));
				}
			} catch (IOException e) {
				throw new IllegalStateException("Could not load properties file from "
						+ resource, e);
			}
		}

		protected Properties loadProperties(Resource resource,
				ConfigurableApplicationContext applicationContext) throws IOException {
			return PropertiesLoaderUtils.loadProperties(resource);
		}
	}

	/**
	 * Strategy to load '.yml' files.
	 */
	private static class YamlLoader extends PropertiesLoader {

		@Override
		public Set<String> getExtensions() {
			return Collections.singleton(".yml");
		}

		@Override
		protected Properties loadProperties(final Resource resource,
				final ConfigurableApplicationContext applicationContext)
				throws IOException {
			YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
			List<DocumentMatcher> matchers = new ArrayList<DocumentMatcher>();
			matchers.add(new DocumentMatcher() {
				@Override
				public MatchStatus matches(Properties properties) {
					String[] profiles = applicationContext.getEnvironment()
							.getActiveProfiles();
					if (profiles.length == 0) {
						profiles = new String[] { "default" };
					}
					return new ArrayDocumentMatcher("spring.profiles", profiles)
							.matches(properties);

				}
			});
			matchers.add(new DocumentMatcher() {
				@Override
				public MatchStatus matches(Properties properties) {
					if (!properties.containsKey("spring.profiles")) {
						Set<String> profiles = StringUtils
								.commaDelimitedListToSet(properties.getProperty(
										"spring.profiles.active", ""));
						for (String profile : profiles) {
							// allow document with no profile to set the active one
							applicationContext.getEnvironment().addActiveProfile(profile);
						}
						// matches default profile
						return MatchStatus.FOUND;
					} else {
						return MatchStatus.NOT_FOUND;
					}
				}
			});
			factory.setMatchDefault(false);
			factory.setDocumentMatchers(matchers);
			factory.setResources(new Resource[] { resource });
			return factory.getObject();
		}
	}

}
