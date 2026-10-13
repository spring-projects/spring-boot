/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.graphql.autoconfigure.security;

import graphql.GraphQL;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.graphql.autoconfigure.reactive.GraphQlWebFluxAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.execution.ReactiveSecurityDataFetcherExceptionResolver;
import org.springframework.graphql.server.webflux.GraphQlHttpHandler;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for enabling Security support for
 * Spring GraphQL with WebFlux.
 *
 * @author Brian Clozel
 * @since 4.0.0
 */
@AutoConfiguration(after = GraphQlWebFluxAutoConfiguration.class)
@ConditionalOnWebApplication(type = Type.REACTIVE)
@ConditionalOnClass({ GraphQL.class, GraphQlHttpHandler.class, EnableWebFluxSecurity.class })
@ConditionalOnBean(GraphQlHttpHandler.class)
public final class GraphQlWebFluxSecurityAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	ReactiveSecurityDataFetcherExceptionResolver reactiveSecurityDataFetcherExceptionResolver() {
		return new ReactiveSecurityDataFetcherExceptionResolver();
	}

}
