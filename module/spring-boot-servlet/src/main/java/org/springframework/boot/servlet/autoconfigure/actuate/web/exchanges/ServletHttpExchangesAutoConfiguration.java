/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.servlet.autoconfigure.actuate.web.exchanges;

import org.springframework.boot.actuate.autoconfigure.web.exchanges.HttpExchangesProperties;
import org.springframework.boot.actuate.web.exchanges.HttpExchange;
import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.servlet.actuate.web.exchanges.HttpExchangesFilter;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} to record {@link HttpExchange HTTP
 * exchanges}.
 *
 * @author Dave Syer
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnBooleanProperty(name = "management.httpexchanges.recording.enabled", matchIfMissing = true)
@ConditionalOnBean(HttpExchangeRepository.class)
@EnableConfigurationProperties(HttpExchangesProperties.class)
public final class ServletHttpExchangesAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	HttpExchangesFilter httpExchangesFilter(HttpExchangeRepository repository, HttpExchangesProperties properties) {
		return new HttpExchangesFilter(repository, properties.getRecording().getInclude());
	}

}
