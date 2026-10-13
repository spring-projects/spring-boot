/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.autoconfigure.web.exchanges;

import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository;
import org.springframework.boot.actuate.web.exchanges.HttpExchangesEndpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for the
 * {@link HttpExchangesEndpoint}.
 *
 * @author Phillip Webb
 * @since 3.0.0
 */
@AutoConfiguration
@ConditionalOnAvailableEndpoint(HttpExchangesEndpoint.class)
public final class HttpExchangesEndpointAutoConfiguration {

	@Bean
	@ConditionalOnBean(HttpExchangeRepository.class)
	@ConditionalOnMissingBean
	HttpExchangesEndpoint httpExchangesEndpoint(HttpExchangeRepository exchangeRepository) {
		return new HttpExchangesEndpoint(exchangeRepository);
	}

}
