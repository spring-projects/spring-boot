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

package org.springframework.boot.context.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.logging.Log;

import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.context.config.ConfigDataEnvironmentContributor.ImportPhase;
import org.springframework.boot.context.config.ConfigDataEnvironmentContributor.Kind;
import org.springframework.boot.context.properties.bind.BindContext;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.PlaceholdersResolver;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.log.LogMessage;
import org.springframework.util.ObjectUtils;

/**
 * An immutable tree structure of {@link ConfigDataEnvironmentContributors} used to
 * process imports.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ConfigDataEnvironmentContributors implements Iterable<ConfigDataEnvironmentContributor> {

	private static final Predicate<ConfigDataEnvironmentContributor> NO_CONTRIBUTOR_FILTER = (contributor) -> true;

	private final Log logger;

	private final ConfigDataEnvironmentContributor root;

	private final ConfigurableBootstrapContext bootstrapContext;

	/**
	 * Create a new {@link ConfigDataEnvironmentContributors} instance.
	 * @param logFactory the log factory
	 * @param bootstrapContext the bootstrap context
	 * @param contributors the initial set of contributors
	 */
	ConfigDataEnvironmentContributors(DeferredLogFactory logFactory, ConfigurableBootstrapContext bootstrapContext,
			List<ConfigDataEnvironmentContributor> contributors) {
		this.logger = logFactory.getLog(getClass());
		this.bootstrapContext = bootstrapContext;
		this.root = ConfigDataEnvironmentContributor.of(contributors);
	}

	/**
     * Constructs a new instance of ConfigDataEnvironmentContributors with the specified logger, bootstrapContext, and root.
     * 
     * @param logger The logger to be used for logging.
     * @param bootstrapContext The bootstrap context for configuring the application.
     * @param root The root environment contributor.
     */
    private ConfigDataEnvironmentContributors(Log logger, ConfigurableBootstrapContext bootstrapContext,
			ConfigDataEnvironmentContributor root) {
		this.logger = logger;
		this.bootstrapContext = bootstrapContext;
		this.root = root;
	}

	/**
	 * Processes imports from all active contributors and return a new
	 * {@link ConfigDataEnvironmentContributors} instance.
	 * @param importer the importer used to import {@link ConfigData}
	 * @param activationContext the current activation context or {@code null} if the
	 * context has not yet been created
	 * @return a {@link ConfigDataEnvironmentContributors} instance with all relevant
	 * imports have been processed
	 */
	ConfigDataEnvironmentContributors withProcessedImports(ConfigDataImporter importer,
			ConfigDataActivationContext activationContext) {
		ImportPhase importPhase = ImportPhase.get(activationContext);
		this.logger.trace(LogMessage.format("Processing imports for phase %s. %s", importPhase,
				(activationContext != null) ? activationContext : "no activation context"));
		ConfigDataEnvironmentContributors result = this;
		int processed = 0;
		while (true) {
			ConfigDataEnvironmentContributor contributor = getNextToProcess(result, activationContext, importPhase);
			if (contributor == null) {
				this.logger.trace(LogMessage.format("Processed imports for of %d contributors", processed));
				return result;
			}
			if (contributor.getKind() == Kind.UNBOUND_IMPORT) {
				ConfigDataEnvironmentContributor bound = contributor.withBoundProperties(result, activationContext);
				result = new ConfigDataEnvironmentContributors(this.logger, this.bootstrapContext,
						result.getRoot().withReplacement(contributor, bound));
				continue;
			}
			ConfigDataLocationResolverContext locationResolverContext = new ContributorConfigDataLocationResolverContext(
					result, contributor, activationContext);
			ConfigDataLoaderContext loaderContext = new ContributorDataLoaderContext(this);
			List<ConfigDataLocation> imports = contributor.getImports();
			this.logger.trace(LogMessage.format("Processing imports %s", imports));
			Map<ConfigDataResolutionResult, ConfigData> imported = importer.resolveAndLoad(activationContext,
					locationResolverContext, loaderContext, imports);
			this.logger.trace(LogMessage.of(() -> getImportedMessage(imported.keySet())));
			ConfigDataEnvironmentContributor contributorAndChildren = contributor.withChildren(importPhase,
					asContributors(imported));
			result = new ConfigDataEnvironmentContributors(this.logger, this.bootstrapContext,
					result.getRoot().withReplacement(contributor, contributorAndChildren));
			processed++;
		}
	}

	/**
     * Returns a message indicating the imported resources.
     * 
     * @param results the set of ConfigDataResolutionResult objects representing the imported resources
     * @return a CharSequence containing the imported resources message
     */
    private CharSequence getImportedMessage(Set<ConfigDataResolutionResult> results) {
		if (results.isEmpty()) {
			return "Nothing imported";
		}
		StringBuilder message = new StringBuilder();
		message.append("Imported " + results.size() + " resource" + ((results.size() != 1) ? "s " : " "));
		message.append(results.stream().map(ConfigDataResolutionResult::getResource).toList());
		return message;
	}

	/**
     * Returns the bootstrap context associated with this ConfigDataEnvironmentContributors instance.
     *
     * @return the bootstrap context
     */
    protected final ConfigurableBootstrapContext getBootstrapContext() {
		return this.bootstrapContext;
	}

	/**
     * Returns the next ConfigDataEnvironmentContributor to process from the given list of contributors.
     * 
     * @param contributors     the list of ConfigDataEnvironmentContributors
     * @param activationContext the ConfigDataActivationContext
     * @param importPhase      the ImportPhase
     * @return the next ConfigDataEnvironmentContributor to process, or null if none found
     */
    private ConfigDataEnvironmentContributor getNextToProcess(ConfigDataEnvironmentContributors contributors,
			ConfigDataActivationContext activationContext, ImportPhase importPhase) {
		for (ConfigDataEnvironmentContributor contributor : contributors.getRoot()) {
			if (contributor.getKind() == Kind.UNBOUND_IMPORT
					|| isActiveWithUnprocessedImports(activationContext, importPhase, contributor)) {
				return contributor;
			}
		}
		return null;
	}

	/**
     * Checks if the given contributor is active with unprocessed imports.
     * 
     * @param activationContext the activation context for the configuration data
     * @param importPhase the import phase for the configuration data
     * @param contributor the environment contributor to check
     * @return {@code true} if the contributor is active with unprocessed imports, {@code false} otherwise
     */
    private boolean isActiveWithUnprocessedImports(ConfigDataActivationContext activationContext,
			ImportPhase importPhase, ConfigDataEnvironmentContributor contributor) {
		return contributor.isActive(activationContext) && contributor.hasUnprocessedImports(importPhase);
	}

	/**
     * Converts the imported {@link ConfigDataResolutionResult} and {@link ConfigData} map into a list of {@link ConfigDataEnvironmentContributor}.
     * Each {@link ConfigDataEnvironmentContributor} represents a resolved configuration data source.
     * 
     * @param imported the map of {@link ConfigDataResolutionResult} and {@link ConfigData} representing the imported configuration data
     * @return the list of {@link ConfigDataEnvironmentContributor} representing the resolved configuration data sources
     */
    private List<ConfigDataEnvironmentContributor> asContributors(
			Map<ConfigDataResolutionResult, ConfigData> imported) {
		List<ConfigDataEnvironmentContributor> contributors = new ArrayList<>(imported.size() * 5);
		imported.forEach((resolutionResult, data) -> {
			ConfigDataLocation location = resolutionResult.getLocation();
			ConfigDataResource resource = resolutionResult.getResource();
			boolean profileSpecific = resolutionResult.isProfileSpecific();
			if (data.getPropertySources().isEmpty()) {
				contributors.add(ConfigDataEnvironmentContributor.ofEmptyLocation(location, profileSpecific));
			}
			else {
				for (int i = data.getPropertySources().size() - 1; i >= 0; i--) {
					contributors.add(ConfigDataEnvironmentContributor.ofUnboundImport(location, resource,
							profileSpecific, data, i));
				}
			}
		});
		return Collections.unmodifiableList(contributors);
	}

	/**
	 * Returns the root contributor.
	 * @return the root contributor.
	 */
	ConfigDataEnvironmentContributor getRoot() {
		return this.root;
	}

	/**
	 * Return a {@link Binder} backed by the contributors.
	 * @param activationContext the activation context
	 * @param options binder options to apply
	 * @return a binder instance
	 */
	Binder getBinder(ConfigDataActivationContext activationContext, BinderOption... options) {
		return getBinder(activationContext, NO_CONTRIBUTOR_FILTER, options);
	}

	/**
	 * Return a {@link Binder} backed by the contributors.
	 * @param activationContext the activation context
	 * @param filter a filter used to limit the contributors
	 * @param options binder options to apply
	 * @return a binder instance
	 */
	Binder getBinder(ConfigDataActivationContext activationContext, Predicate<ConfigDataEnvironmentContributor> filter,
			BinderOption... options) {
		return getBinder(activationContext, filter, asBinderOptionsSet(options));
	}

	/**
     * Converts an array of BinderOption objects into a Set of BinderOption objects.
     * 
     * @param options the array of BinderOption objects to be converted
     * @return a Set of BinderOption objects
     */
    private Set<BinderOption> asBinderOptionsSet(BinderOption... options) {
		return ObjectUtils.isEmpty(options) ? EnumSet.noneOf(BinderOption.class)
				: EnumSet.copyOf(Arrays.asList(options));
	}

	/**
     * Returns a Binder object based on the provided activation context, filter, and options.
     * 
     * @param activationContext The activation context for the binder.
     * @param filter The filter to apply to the environment contributors.
     * @param options The set of options for the binder.
     * @return A Binder object.
     */
    private Binder getBinder(ConfigDataActivationContext activationContext,
			Predicate<ConfigDataEnvironmentContributor> filter, Set<BinderOption> options) {
		boolean failOnInactiveSource = options.contains(BinderOption.FAIL_ON_BIND_TO_INACTIVE_SOURCE);
		Iterable<ConfigurationPropertySource> sources = () -> getBinderSources(
				filter.and((contributor) -> failOnInactiveSource || contributor.isActive(activationContext)));
		PlaceholdersResolver placeholdersResolver = new ConfigDataEnvironmentContributorPlaceholdersResolver(this.root,
				activationContext, null, failOnInactiveSource);
		BindHandler bindHandler = !failOnInactiveSource ? null : new InactiveSourceChecker(activationContext);
		return new Binder(sources, placeholdersResolver, null, null, bindHandler);
	}

	/**
     * Returns an iterator of ConfigurationPropertySource objects obtained from the root contributors,
     * filtered by the given predicate.
     *
     * @param filter the predicate used to filter the contributors
     * @return an iterator of ConfigurationPropertySource objects
     */
    private Iterator<ConfigurationPropertySource> getBinderSources(Predicate<ConfigDataEnvironmentContributor> filter) {
		return this.root.stream()
			.filter(this::hasConfigurationPropertySource)
			.filter(filter)
			.map(ConfigDataEnvironmentContributor::getConfigurationPropertySource)
			.iterator();
	}

	/**
     * Checks if the given ConfigDataEnvironmentContributor has a configuration property source.
     * 
     * @param contributor the ConfigDataEnvironmentContributor to check
     * @return true if the contributor has a configuration property source, false otherwise
     */
    private boolean hasConfigurationPropertySource(ConfigDataEnvironmentContributor contributor) {
		return contributor.getConfigurationPropertySource() != null;
	}

	/**
     * Returns an iterator over the elements in this ConfigDataEnvironmentContributors object.
     *
     * @return an iterator over the elements in this ConfigDataEnvironmentContributors object
     */
    @Override
	public Iterator<ConfigDataEnvironmentContributor> iterator() {
		return this.root.iterator();
	}

	/**
	 * {@link ConfigDataLocationResolverContext} for a contributor.
	 */
	private static class ContributorDataLoaderContext implements ConfigDataLoaderContext {

		private final ConfigDataEnvironmentContributors contributors;

		/**
         * Constructs a new instance of ContributorDataLoaderContext with the specified ConfigDataEnvironmentContributors.
         * 
         * @param contributors the ConfigDataEnvironmentContributors to be used by the ContributorDataLoaderContext
         */
        ContributorDataLoaderContext(ConfigDataEnvironmentContributors contributors) {
			this.contributors = contributors;
		}

		/**
         * Returns the bootstrap context of the ContributorDataLoaderContext.
         * 
         * @return the bootstrap context of the ContributorDataLoaderContext
         */
        @Override
		public ConfigurableBootstrapContext getBootstrapContext() {
			return this.contributors.getBootstrapContext();
		}

	}

	/**
	 * {@link ConfigDataLocationResolverContext} for a contributor.
	 */
	private static class ContributorConfigDataLocationResolverContext implements ConfigDataLocationResolverContext {

		private final ConfigDataEnvironmentContributors contributors;

		private final ConfigDataEnvironmentContributor contributor;

		private final ConfigDataActivationContext activationContext;

		private volatile Binder binder;

		/**
         * Constructs a new instance of ContributorConfigDataLocationResolverContext.
         * 
         * @param contributors the ConfigDataEnvironmentContributors object
         * @param contributor the ConfigDataEnvironmentContributor object
         * @param activationContext the ConfigDataActivationContext object
         */
        ContributorConfigDataLocationResolverContext(ConfigDataEnvironmentContributors contributors,
				ConfigDataEnvironmentContributor contributor, ConfigDataActivationContext activationContext) {
			this.contributors = contributors;
			this.contributor = contributor;
			this.activationContext = activationContext;
		}

		/**
         * Returns the binder associated with this ContributorConfigDataLocationResolverContext.
         * If the binder is null, it retrieves the binder from the contributors using the activation context.
         * 
         * @return the binder associated with this ContributorConfigDataLocationResolverContext
         */
        @Override
		public Binder getBinder() {
			Binder binder = this.binder;
			if (binder == null) {
				binder = this.contributors.getBinder(this.activationContext);
				this.binder = binder;
			}
			return binder;
		}

		/**
         * Returns the parent ConfigDataResource of this ContributorConfigDataLocationResolverContext.
         * 
         * @return the parent ConfigDataResource
         */
        @Override
		public ConfigDataResource getParent() {
			return this.contributor.getResource();
		}

		/**
         * Returns the bootstrap context of the contributor.
         *
         * @return the bootstrap context of the contributor
         */
        @Override
		public ConfigurableBootstrapContext getBootstrapContext() {
			return this.contributors.getBootstrapContext();
		}

	}

	/**
     * InactiveSourceChecker class.
     */
    private class InactiveSourceChecker implements BindHandler {

		private final ConfigDataActivationContext activationContext;

		/**
         * Constructs a new InactiveSourceChecker with the specified activation context.
         * 
         * @param activationContext the activation context to be used for checking inactive sources
         */
        InactiveSourceChecker(ConfigDataActivationContext activationContext) {
			this.activationContext = activationContext;
		}

		/**
         * This method is called when the binding process is successful. It checks if the given configuration property name is active
         * for each ConfigDataEnvironmentContributor in the ConfigDataEnvironmentContributors list. If a contributor is not active,
         * it throws an InactiveConfigDataAccessException if the property is found. Otherwise, it returns the result of the binding process.
         * 
         * @param name     the name of the configuration property
         * @param target   the bindable target
         * @param context  the bind context
         * @param result   the result of the binding process
         * @return         the result of the binding process
         */
        @Override
		public Object onSuccess(ConfigurationPropertyName name, Bindable<?> target, BindContext context,
				Object result) {
			for (ConfigDataEnvironmentContributor contributor : ConfigDataEnvironmentContributors.this) {
				if (!contributor.isActive(this.activationContext)) {
					InactiveConfigDataAccessException.throwIfPropertyFound(contributor, name);
				}
			}
			return result;
		}

	}

	/**
	 * Binder options that can be used with
	 * {@link ConfigDataEnvironmentContributors#getBinder(ConfigDataActivationContext, BinderOption...)}.
	 */
	enum BinderOption {

		/**
		 * Throw an exception if an inactive contributor contains a bound value.
		 */
		FAIL_ON_BIND_TO_INACTIVE_SOURCE

	}

}
