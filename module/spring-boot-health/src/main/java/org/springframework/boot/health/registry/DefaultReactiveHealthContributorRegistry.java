/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.registry;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.health.contributor.ReactiveHealthContributor;
import org.springframework.boot.health.contributor.ReactiveHealthContributors.Entry;

/**
 * Default {@link ReactiveHealthContributorRegistry} implementation.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public class DefaultReactiveHealthContributorRegistry extends AbstractRegistry<ReactiveHealthContributor, Entry>
		implements ReactiveHealthContributorRegistry {

	/**
	 * Create a new empty {@link DefaultReactiveHealthContributorRegistry} instance.
	 */
	public DefaultReactiveHealthContributorRegistry() {
		this(null, null);
	}

	/**
	 * Create a new {@link DefaultReactiveHealthContributorRegistry} instance.
	 * @param nameValidators the name validators to apply
	 * @param initialRegistrations callback to setup any initial registrations
	 */
	public DefaultReactiveHealthContributorRegistry(
			@Nullable Collection<? extends HealthContributorNameValidator> nameValidators,
			@Nullable Consumer<BiConsumer<String, ReactiveHealthContributor>> initialRegistrations) {
		super(Entry::new, nameValidators, initialRegistrations);
	}

	@Override
	public @Nullable ReactiveHealthContributor getContributor(String name) {
		return super.getContributor(name);
	}

	@Override
	public Stream<Entry> stream() {
		return super.stream();
	}

	@Override
	public void registerContributor(String name, ReactiveHealthContributor contributor) {
		super.registerContributor(name, contributor);
	}

	@Override
	public @Nullable ReactiveHealthContributor unregisterContributor(String name) {
		return super.unregisterContributor(name);
	}

}
