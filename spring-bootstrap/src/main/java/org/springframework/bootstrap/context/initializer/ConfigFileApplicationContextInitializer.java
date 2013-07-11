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

import java.util.ArrayList;
import java.util.List;

import org.springframework.bootstrap.config.SpringProfileDocumentMatcher;
import org.springframework.bootstrap.config.PropertiesPropertySourceLoader;
import org.springframework.bootstrap.config.PropertySourceLoader;
import org.springframework.bootstrap.config.YamlPropertySourceLoader;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.CommandLinePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
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
 * <p>
 * Alternative locations and names can be specified using
 * {@link #setSearchLocations(String[])} and {@link #setName(String)}.
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
		ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {

	private static final String LOCATION_VARIABLE = "${spring.config.location}";

	private String[] searchLocations = new String[] { "classpath:", "file:./",
			"classpath:config/", "file:./config/" };

	private String name = "${spring.config.name:application}";

	private int order = Integer.MIN_VALUE + 10;

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		List<String> candidates = getCandidateLocations();

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

	private List<String> getCandidateLocations() {
		List<String> candidates = new ArrayList<String>();
		for (String searchLocation : this.searchLocations) {
			for (String extension : new String[] { ".properties", ".yml" }) {
				String location = searchLocation + this.name + extension;
				candidates.add(location);
			}
		}
		candidates.add(LOCATION_VARIABLE);
		return candidates;
	}

	private void load(ConfigurableApplicationContext applicationContext, String location,
			String profile) {

		ConfigurableEnvironment environment = applicationContext.getEnvironment();
		location = environment.resolvePlaceholders(location);
		String suffix = "." + StringUtils.getFilenameExtension(location);
		if (StringUtils.hasLength(profile)) {
			location = location.replace(suffix, "-" + profile + suffix);
		}
		PropertySourceLoader[] loaders = {
				new PropertiesPropertySourceLoader(),
				new YamlPropertySourceLoader(new SpringProfileDocumentMatcher(environment),
						new ProfileSettingDocumentMatcher(environment)) };
		for (PropertySourceLoader loader : loaders) {
			Resource resource = applicationContext.getResource(location);
			if (resource != null && resource.exists() && loader.supports(resource)) {
				PropertySource<?> propertySource = loader.load(resource, environment);
				MutablePropertySources propertySources = environment.getPropertySources();
				if (propertySources
						.contains(CommandLinePropertySource.COMMAND_LINE_PROPERTY_SOURCE_NAME)) {
					propertySources.addAfter(
							CommandLinePropertySource.COMMAND_LINE_PROPERTY_SOURCE_NAME,
							propertySource);

				} else {
					propertySources.addFirst(propertySource);
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
		this.searchLocations = (searchLocations == null ? null : searchLocations.clone());
	}

}
