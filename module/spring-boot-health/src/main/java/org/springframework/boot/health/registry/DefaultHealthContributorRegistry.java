/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.registry;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.boot.health.contributor.HealthContributors.Entry;

/**
 * Default {@link HealthContributorRegistry} implementation.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public class DefaultHealthContributorRegistry extends AbstractRegistry<HealthContributor, Entry>
		implements HealthContributorRegistry {

	/**
	 * Create a new empty {@link DefaultHealthContributorRegistry} instance.
	 */
	public DefaultHealthContributorRegistry() {
		this(null, null);
	}

	/**
	 * Create a new {@link DefaultHealthContributorRegistry} instance.
	 * @param nameValidators the name validators to apply
	 * @param initialRegistrations callback to setup any initial registrations
	 */
	public DefaultHealthContributorRegistry(
			@Nullable Collection<? extends HealthContributorNameValidator> nameValidators,
			@Nullable Consumer<BiConsumer<String, HealthContributor>> initialRegistrations) {
		super(Entry::new, nameValidators, initialRegistrations);
	}

	@Override
	public @Nullable HealthContributor getContributor(String name) {
		return super.getContributor(name);
	}

	@Override
	public Stream<Entry> stream() {
		return super.stream();
	}

	@Override
	public void registerContributor(String name, HealthContributor contributor) {
		super.registerContributor(name, contributor);
	}

	@Override
	public @Nullable HealthContributor unregisterContributor(String name) {
		return super.unregisterContributor(name);
	}

}
