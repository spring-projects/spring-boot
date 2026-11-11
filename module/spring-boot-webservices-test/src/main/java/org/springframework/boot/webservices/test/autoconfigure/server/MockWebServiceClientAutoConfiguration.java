/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webservices.test.autoconfigure.server;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.ws.test.server.MockWebServiceClient;

/**
 * Auto-configuration for {@link MockWebServiceClient} support.
 *
 * @author Daniil Razorenov
 * @since 4.0.0
 * @see AutoConfigureMockWebServiceClient
 */
@AutoConfiguration
@ConditionalOnClass(MockWebServiceClient.class)
public final class MockWebServiceClientAutoConfiguration {

	@Bean
	MockWebServiceClient mockWebServiceClient(ApplicationContext applicationContext) {
		return MockWebServiceClient.createClient(applicationContext);
	}

}
