/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.test.autoconfigure.webflux;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.web.reactive.server.MockServerConfigurer;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Auto-configuration for Spring Security's {@link WebTestClient} integration.
 *
 * @author Madhura Bhave
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass(SecurityMockServerConfigurers.class)
public final class SecurityWebTestClientAutoConfiguration {

	@Bean
	MockServerConfigurer mockServerConfigurer() {
		return SecurityMockServerConfigurers.springSecurity();
	}

}
