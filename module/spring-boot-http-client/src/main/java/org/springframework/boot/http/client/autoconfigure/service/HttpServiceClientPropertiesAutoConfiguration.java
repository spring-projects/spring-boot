/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.http.client.autoconfigure.service;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link HttpServiceClientProperties}.
 *
 * @author Phillip Webb
 * @since 4.0.2
 */
@AutoConfiguration
public final class HttpServiceClientPropertiesAutoConfiguration {

	@Bean
	HttpServiceClientProperties httpServiceClientProperties(Environment environment) {
		return HttpServiceClientProperties.bind(environment);
	}

}
