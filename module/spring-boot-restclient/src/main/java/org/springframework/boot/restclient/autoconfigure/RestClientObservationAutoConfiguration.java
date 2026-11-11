/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.restclient.autoconfigure;

import io.micrometer.observation.ObservationRegistry;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationProperties;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.boot.restclient.observation.ObservationRestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.observation.ClientRequestObservationConvention;
import org.springframework.http.client.observation.DefaultClientRequestObservationConvention;
import org.springframework.web.client.RestClient;

/**
 * Configure the instrumentation of {@link RestClient}.
 *
 * @author Moritz Halbritter
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass({ RestClient.class, ObservationRestClientCustomizer.class, ObservationRegistry.class,
		ObservationProperties.class })
@ConditionalOnBean({ RestClient.Builder.class, ObservationRegistry.class })
@EnableConfigurationProperties(ObservationProperties.class)
public final class RestClientObservationAutoConfiguration {

	@Bean
	RestClientCustomizer observationRestClientCustomizer(ObservationRegistry observationRegistry,
			ObjectProvider<ClientRequestObservationConvention> customConvention,
			ObservationProperties observationProperties) {
		String name = observationProperties.getHttp().getClient().getRequests().getName();
		ClientRequestObservationConvention observationConvention = customConvention
			.getIfAvailable(() -> new DefaultClientRequestObservationConvention(name));
		return new ObservationRestClientCustomizer(observationRegistry, observationConvention);
	}

}
