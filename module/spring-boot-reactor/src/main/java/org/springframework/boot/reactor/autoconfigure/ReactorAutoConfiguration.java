/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.reactor.autoconfigure;

import reactor.core.publisher.Hooks;

import org.springframework.boot.LazyInitializationExcludeFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Reactor.
 *
 * @author Brian Clozel
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass(Hooks.class)
@EnableConfigurationProperties(ReactorProperties.class)
public final class ReactorAutoConfiguration {

	ReactorAutoConfiguration(ReactorProperties properties) {
		if (properties.getContextPropagation() == ReactorProperties.ContextPropagationMode.AUTO) {
			Hooks.enableAutomaticContextPropagation();
		}
	}

	@Bean
	static LazyInitializationExcludeFilter reactorAutoConfigurationLazyInitializationExcludeFilter() {
		return LazyInitializationExcludeFilter.forBeanTypes(ReactorAutoConfiguration.class);
	}

}
