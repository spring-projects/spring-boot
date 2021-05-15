/*
 * Copyright 2012-2021 the original author or authors.
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
import java.util.stream.Collectors;

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
	 * context has not get been created
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
				Iterable<ConfigurationPropertySource> sources = Collections
						.singleton(contributor.getConfigurationPropertySource());
				PlaceholdersResolver placeholdersResolver = new ConfigDataEnvironmentContributorPlaceholdersResolver(
						result, activationContext, true);
				Binder binder = new Binder(sources, placeholdersResolver, null, null, null);
				ConfigDataEnvironmentContributor bound = contributor.withBoundProperties(binder);
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

	private CharSequence getImportedMessage(Set<ConfigDataResolutionResult> results) {
		if (results.isEmpty()) {
			return "Nothing imported";
		}
		StringBuilder message = new StringBuilder();
		message.append("Imported " + results.size() + " resource" + ((results.size() != 1) ? "s " : " "));
		message.append(results.stream().map(ConfigDataResolutionResult::getResource).collect(Collectors.toList()));
		return message;
	}

	protected final ConfigurableBootstrapContext getBootstrapContext() {
		return this.bootstrapContext;
	}

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

	private boolean isActiveWithUnprocessedImports(ConfigDataActivationContext activationContext,
			ImportPhase importPhase, ConfigDataEnvironmentContributor contributor) {
		return contributor.isActive(activationContext) && contributor.hasUnprocessedImports(importPhase);
	}

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

	private Set<BinderOption> asBinderOptionsSet(BinderOption... options) {
		return ObjectUtils.isEmpty(options) ? EnumSet.noneOf(BinderOption.class)
				: EnumSet.copyOf(Arrays.asList(options));
	}

	private Binder getBinder(ConfigDataActivationContext activationContext,
			Predicate<ConfigDataEnvironmentContributor> filter, Set<BinderOption> options) {
		boolean failOnInactiveSource = options.contains(BinderOption.FAIL_ON_BIND_TO_INACTIVE_SOURCE);
		Iterable<ConfigurationPropertySource> sources = () -> getBinderSources(activationContext,
				filter.and((contributor) -> failOnInactiveSource || contributor.isActive(activationContext)));
		PlaceholdersResolver placeholdersResolver = new ConfigDataEnvironmentContributorPlaceholdersResolver(this.root,
				activationContext, failOnInactiveSource);
		BindHandler bindHandler = !failOnInactiveSource ? null : new InactiveSourceChecker(activationContext);
		return new Binder(sources, placeholdersResolver, null, null, bindHandler);
	}

	private Iterator<ConfigurationPropertySource> getBinderSources(ConfigDataActivationContext activationContext,
			Predicate<ConfigDataEnvironmentContributor> filter) {
		return this.root.stream().filter(this::hasConfigurationPropertySource).filter(filter)
				.map(ConfigDataEnvironmentContributor::getConfigurationPropertySource).iterator();
	}

	private boolean hasConfigurationPropertySource(ConfigDataEnvironmentContributor contributor) {
		return contributor.getConfigurationPropertySource() != null;
	}

	@Override
	public Iterator<ConfigDataEnvironmentContributor> iterator() {
		return this.root.iterator();
	}

	/**
	 * {@link ConfigDataLocationResolverContext} for a contributor.
	 */
	private static class ContributorDataLoaderContext implements ConfigDataLoaderContext {

		private final ConfigDataEnvironmentContributors contributors;

		ContributorDataLoaderContext(ConfigDataEnvironmentContributors contributors) {
			this.contributors = contributors;
		}

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

		ContributorConfigDataLocationResolverContext(ConfigDataEnvironmentContributors contributors,
				ConfigDataEnvironmentContributor contributor, ConfigDataActivationContext activationContext) {
			this.contributors = contributors;
			this.contributor = contributor;
			this.activationContext = activationContext;
		}

		@Override
		public Binder getBinder() {
			Binder binder = this.binder;
			if (binder == null) {
				binder = this.contributors.getBinder(this.activationContext);
				this.binder = binder;
			}
			return binder;
		}

		@Override
		public ConfigDataResource getParent() {
			return this.contributor.getResource();
		}

		@Override
		public ConfigurableBootstrapContext getBootstrapContext() {
			return this.contributors.getBootstrapContext();
		}

	}

	private class InactiveSourceChecker implements BindHandler {

		private final ConfigDataActivationContext activationContext;

		InactiveSourceChecker(ConfigDataActivationContext activationContext) {
			this.activationContext = activationContext;
		}

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
		FAIL_ON_BIND_TO_INACTIVE_SOURCE;

	}

}
