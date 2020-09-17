/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.context.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;

import org.springframework.boot.BootstrapRegistry.InstanceSupplier;
import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.DefaultPropertiesPropertySource;
import org.springframework.boot.context.config.ConfigDataEnvironmentContributors.BinderOption;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.PlaceholdersResolver;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.log.LogMessage;
import org.springframework.util.StringUtils;

/**
 * Wrapper around a {@link ConfigurableEnvironment} that can be used to import and apply
 * {@link ConfigData}. Configures the initial set of
 * {@link ConfigDataEnvironmentContributors} by wrapping property sources from the Spring
 * {@link Environment} and adding the initial set of imports.
 * <p>
 * The initial imports can be influenced via the {@link #LOCATION_PROPERTY},
 * {@value #ADDITIONAL_LOCATION_PROPERTY} and {@value #IMPORT_PROPERTY} properties. If not
 * explicit properties are set, the {@link #DEFAULT_SEARCH_LOCATIONS} will be used.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ConfigDataEnvironment {

	/**
	 * Property used override the imported locations.
	 */
	static final String LOCATION_PROPERTY = "spring.config.location";

	/**
	 * Property used to provide additional locations to import.
	 */
	static final String ADDITIONAL_LOCATION_PROPERTY = "spring.config.additional-location";

	/**
	 * Property used to provide additional locations to import.
	 */
	static final String IMPORT_PROPERTY = "spring.config.import";

	/**
	 * Property used to determine what action to take when a
	 * {@code ConfigDataLocationNotFoundException} is thrown.
	 * @see ConfigDataLocationNotFoundAction
	 */
	static final String ON_LOCATION_NOT_FOUND_PROPERTY = "spring.config.on-location-not-found";

	/**
	 * Default search locations used if not {@link #LOCATION_PROPERTY} is found.
	 */
	static final String[] DEFAULT_SEARCH_LOCATIONS = { "optional:classpath:/", "optional:classpath:/config/",
			"optional:file:./", "optional:file:./config/*/", "optional:file:./config/" };

	private static final String[] EMPTY_LOCATIONS = new String[0];

	private static final ConfigurationPropertyName INCLUDE_PROFILES = ConfigurationPropertyName
			.of(Profiles.INCLUDE_PROFILES_PROPERTY_NAME);

	private static final Bindable<List<String>> STRING_LIST = Bindable.listOf(String.class);

	private final DeferredLogFactory logFactory;

	private final Log logger;

	private final ConfigurableBootstrapContext bootstrapContext;

	private final ConfigurableEnvironment environment;

	private final ConfigDataLocationResolvers resolvers;

	private final Collection<String> additionalProfiles;

	private final ConfigDataLoaders loaders;

	private final ConfigDataEnvironmentContributors contributors;

	/**
	 * Create a new {@link ConfigDataEnvironment} instance.
	 * @param logFactory the deferred log factory
	 * @param bootstrapContext the bootstrap context
	 * @param environment the Spring {@link Environment}.
	 * @param resourceLoader {@link ResourceLoader} to load resource locations
	 * @param additionalProfiles any additional profiles to activate
	 */
	ConfigDataEnvironment(DeferredLogFactory logFactory, ConfigurableBootstrapContext bootstrapContext,
			ConfigurableEnvironment environment, ResourceLoader resourceLoader, Collection<String> additionalProfiles) {
		Binder binder = Binder.get(environment);
		UseLegacyConfigProcessingException.throwIfRequested(binder);
		ConfigDataLocationNotFoundAction locationNotFoundAction = binder
				.bind(ON_LOCATION_NOT_FOUND_PROPERTY, ConfigDataLocationNotFoundAction.class)
				.orElse(ConfigDataLocationNotFoundAction.FAIL);
		this.logFactory = logFactory;
		this.logger = logFactory.getLog(getClass());
		this.bootstrapContext = bootstrapContext;
		this.environment = environment;
		this.resolvers = createConfigDataLocationResolvers(logFactory, bootstrapContext, locationNotFoundAction, binder,
				resourceLoader);
		this.additionalProfiles = additionalProfiles;
		this.loaders = new ConfigDataLoaders(logFactory, bootstrapContext, locationNotFoundAction);
		this.contributors = createContributors(binder);
	}

	protected ConfigDataLocationResolvers createConfigDataLocationResolvers(DeferredLogFactory logFactory,
			ConfigurableBootstrapContext bootstrapContext, ConfigDataLocationNotFoundAction locationNotFoundAction,
			Binder binder, ResourceLoader resourceLoader) {
		return new ConfigDataLocationResolvers(logFactory, bootstrapContext, locationNotFoundAction, binder,
				resourceLoader);
	}

	private ConfigDataEnvironmentContributors createContributors(Binder binder) {
		this.logger.trace("Building config data environment contributors");
		MutablePropertySources propertySources = this.environment.getPropertySources();
		List<ConfigDataEnvironmentContributor> contributors = new ArrayList<>(propertySources.size() + 10);
		PropertySource<?> defaultPropertySource = null;
		for (PropertySource<?> propertySource : propertySources) {
			if (DefaultPropertiesPropertySource.hasMatchingName(propertySource)) {
				defaultPropertySource = propertySource;
			}
			else {
				this.logger.trace(LogMessage.format("Creating wrapped config data contributor for '%s'",
						propertySource.getName()));
				contributors.add(ConfigDataEnvironmentContributor.ofExisting(propertySource));
			}
		}
		contributors.addAll(getInitialImportContributors(binder));
		if (defaultPropertySource != null) {
			this.logger.trace("Creating wrapped config data contributor for default property source");
			contributors.add(ConfigDataEnvironmentContributor.ofExisting(defaultPropertySource));
		}
		return new ConfigDataEnvironmentContributors(this.logFactory, this.bootstrapContext, contributors);
	}

	ConfigDataEnvironmentContributors getContributors() {
		return this.contributors;
	}

	private List<ConfigDataEnvironmentContributor> getInitialImportContributors(Binder binder) {
		List<ConfigDataEnvironmentContributor> initialContributors = new ArrayList<>();
		addInitialImportContributors(initialContributors,
				binder.bind(IMPORT_PROPERTY, String[].class).orElse(EMPTY_LOCATIONS));
		addInitialImportContributors(initialContributors,
				binder.bind(ADDITIONAL_LOCATION_PROPERTY, String[].class).orElse(EMPTY_LOCATIONS));
		addInitialImportContributors(initialContributors,
				binder.bind(LOCATION_PROPERTY, String[].class).orElse(DEFAULT_SEARCH_LOCATIONS));
		return initialContributors;
	}

	private void addInitialImportContributors(List<ConfigDataEnvironmentContributor> initialContributors,
			String[] locations) {
		for (int i = locations.length - 1; i >= 0; i--) {
			initialContributors.add(createInitialImportContributor(locations[i]));
		}
	}

	private ConfigDataEnvironmentContributor createInitialImportContributor(String location) {
		this.logger.trace(LogMessage.format("Adding initial config data import from location '%s'", location));
		return ConfigDataEnvironmentContributor.ofInitialImport(location);
	}

	/**
	 * Process all contributions and apply any newly imported property sources to the
	 * {@link Environment}.
	 */
	void processAndApply() {
		ConfigDataImporter importer = new ConfigDataImporter(this.resolvers, this.loaders);
		this.bootstrapContext.register(Binder.class, InstanceSupplier
				.from(() -> this.contributors.getBinder(null, BinderOption.FAIL_ON_BIND_TO_INACTIVE_SOURCE)));
		ConfigDataEnvironmentContributors contributors = processInitial(this.contributors, importer);
		Binder initialBinder = contributors.getBinder(null, BinderOption.FAIL_ON_BIND_TO_INACTIVE_SOURCE);
		this.bootstrapContext.register(Binder.class, InstanceSupplier.of(initialBinder));
		ConfigDataActivationContext activationContext = createActivationContext(initialBinder);
		contributors = processWithoutProfiles(contributors, importer, activationContext);
		activationContext = withProfiles(contributors, activationContext);
		contributors = processWithProfiles(contributors, importer, activationContext);
		applyToEnvironment(contributors, activationContext);
	}

	private ConfigDataEnvironmentContributors processInitial(ConfigDataEnvironmentContributors contributors,
			ConfigDataImporter importer) {
		this.logger.trace("Processing initial config data environment contributors without activation context");
		return contributors.withProcessedImports(importer, null);
	}

	private ConfigDataActivationContext createActivationContext(Binder initialBinder) {
		this.logger.trace("Creating config data activation context from initial contributions");
		try {
			return new ConfigDataActivationContext(this.environment, initialBinder);
		}
		catch (BindException ex) {
			if (ex.getCause() instanceof InactiveConfigDataAccessException) {
				throw (InactiveConfigDataAccessException) ex.getCause();
			}
			throw ex;
		}
	}

	private ConfigDataEnvironmentContributors processWithoutProfiles(ConfigDataEnvironmentContributors contributors,
			ConfigDataImporter importer, ConfigDataActivationContext activationContext) {
		this.logger.trace("Processing config data environment contributors with initial activation context");
		return contributors.withProcessedImports(importer, activationContext);
	}

	private ConfigDataActivationContext withProfiles(ConfigDataEnvironmentContributors contributors,
			ConfigDataActivationContext activationContext) {
		this.logger.trace("Deducing profiles from current config data environment contributors");
		Binder binder = contributors.getBinder(activationContext, BinderOption.FAIL_ON_BIND_TO_INACTIVE_SOURCE);
		try {
			Set<String> additionalProfiles = new LinkedHashSet<>(this.additionalProfiles);
			additionalProfiles.addAll(getIncludedProfiles(contributors, activationContext));
			Profiles profiles = new Profiles(this.environment, binder, additionalProfiles);
			return activationContext.withProfiles(profiles);
		}
		catch (BindException ex) {
			if (ex.getCause() instanceof InactiveConfigDataAccessException) {
				throw (InactiveConfigDataAccessException) ex.getCause();
			}
			throw ex;
		}
	}

	private Collection<? extends String> getIncludedProfiles(ConfigDataEnvironmentContributors contributors,
			ConfigDataActivationContext activationContext) {
		PlaceholdersResolver placeholdersResolver = new ConfigDataEnvironmentContributorPlaceholdersResolver(
				contributors, activationContext, true);
		Set<String> result = new LinkedHashSet<>();
		for (ConfigDataEnvironmentContributor contributor : contributors) {
			ConfigurationPropertySource source = contributor.getConfigurationPropertySource();
			if (source == null) {
				continue;
			}
			Binder binder = new Binder(Collections.singleton(source), placeholdersResolver);
			binder.bind(INCLUDE_PROFILES, STRING_LIST).ifBound((includes) -> {
				if (!contributor.isActive(activationContext)) {
					InactiveConfigDataAccessException.throwIfPropertyFound(contributor, INCLUDE_PROFILES);
				}
				result.addAll(includes);
			});
		}
		return result;
	}

	private ConfigDataEnvironmentContributors processWithProfiles(ConfigDataEnvironmentContributors contributors,
			ConfigDataImporter importer, ConfigDataActivationContext activationContext) {
		this.logger.trace("Processing config data environment contributors with profile activation context");
		return contributors.withProcessedImports(importer, activationContext);
	}

	private void applyToEnvironment(ConfigDataEnvironmentContributors contributors,
			ConfigDataActivationContext activationContext) {
		checkForInvalidProperties(contributors);
		MutablePropertySources propertySources = this.environment.getPropertySources();
		this.logger.trace("Applying config data environment contributions");
		for (ConfigDataEnvironmentContributor contributor : contributors) {
			if (contributor.getKind() == ConfigDataEnvironmentContributor.Kind.BOUND_IMPORT
					&& contributor.getPropertySource() != null) {
				if (!contributor.isActive(activationContext)) {
					this.logger.trace(LogMessage.format("Skipping inactive property source '%s'",
							contributor.getPropertySource().getName()));
				}
				else {
					this.logger.trace(LogMessage.format("Adding imported property source '%s'",
							contributor.getPropertySource().getName()));
					propertySources.addLast(contributor.getPropertySource());
				}
			}
		}
		DefaultPropertiesPropertySource.moveToEnd(propertySources);
		Profiles profiles = activationContext.getProfiles();
		this.logger.trace(LogMessage.format("Setting default profiles: %s", profiles.getDefault()));
		this.environment.setDefaultProfiles(StringUtils.toStringArray(profiles.getDefault()));
		this.logger.trace(LogMessage.format("Setting active profiles: %s", profiles.getActive()));
		this.environment.setActiveProfiles(StringUtils.toStringArray(profiles.getActive()));
	}

	private void checkForInvalidProperties(ConfigDataEnvironmentContributors contributors) {
		for (ConfigDataEnvironmentContributor contributor : contributors) {
			InvalidConfigDataPropertyException.throwOrWarn(this.logger, contributor);
		}
	}

}
