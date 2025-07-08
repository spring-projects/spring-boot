/*
 * Copyright 2012-present the original author or authors.
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

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link HealthContributorRegistry} and {@link ReactiveHealthContributorRegistry}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
@AutoConfiguration
public class HealthContributorRegistryAutoConfiguration {

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
