/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.autoconfigure.registry;

import java.util.List;
import java.util.Map;

import reactor.core.publisher.Flux;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.boot.health.contributor.ReactiveHealthContributor;
import org.springframework.boot.health.registry.DefaultHealthContributorRegistry;
import org.springframework.boot.health.registry.DefaultReactiveHealthContributorRegistry;
import org.springframework.boot.health.registry.HealthContributorNameValidator;
import org.springframework.boot.health.registry.HealthContributorRegistry;
import org.springframework.boot.health.registry.ReactiveHealthContributorRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link HealthContributorRegistry} and {@link ReactiveHealthContributorRegistry}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
@AutoConfiguration
public final class HealthContributorRegistryAutoConfiguration {

	HealthContributorRegistryAutoConfiguration() {
	}

	@Bean
	@ConditionalOnMissingBean(HealthContributorRegistry.class)
	DefaultHealthContributorRegistry healthContributorRegistry(Map<String, HealthContributor> contributorBeans,
			ObjectProvider<HealthContributorNameGenerator> nameGeneratorProvider,
			List<HealthContributorNameValidator> nameValidators) {
		HealthContributorNameGenerator nameGenerator = nameGeneratorProvider
			.getIfAvailable(HealthContributorNameGenerator::withoutStandardSuffixes);
		return new DefaultHealthContributorRegistry(nameValidators, nameGenerator.registrar(contributorBeans));
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(Flux.class)
	static class ReactiveHealthContributorRegistryConfiguration {

		@Bean
		@ConditionalOnMissingBean(ReactiveHealthContributorRegistry.class)
		DefaultReactiveHealthContributorRegistry reactiveHealthContributorRegistry(
				Map<String, ReactiveHealthContributor> contributorBeans,
				ObjectProvider<HealthContributorNameGenerator> nameGeneratorProvider,
				List<HealthContributorNameValidator> nameValidators) {
			HealthContributorNameGenerator nameGenerator = nameGeneratorProvider
				.getIfAvailable(HealthContributorNameGenerator::withoutStandardSuffixes);
			return new DefaultReactiveHealthContributorRegistry(nameValidators,
					nameGenerator.registrar(contributorBeans));
		}

	}

}
