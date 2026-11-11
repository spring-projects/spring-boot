/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webservices.test.autoconfigure.client;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.test.client.MockWebServiceMessageSender;
import org.springframework.ws.test.client.MockWebServiceServer;

/**
 * Auto-configuration for {@link MockWebServiceServer} support.
 *
 * @author Dmytro Nosan
 * @since 2.3.0
 * @see AutoConfigureMockWebServiceServer
 */
@AutoConfiguration
@ConditionalOnBooleanProperty("spring.test.webservice.client.mockserver.enabled")
@ConditionalOnClass({ MockWebServiceServer.class, WebServiceTemplate.class })
public final class MockWebServiceServerAutoConfiguration {

	@Bean
	TestMockWebServiceServer mockWebServiceServer() {
		return new TestMockWebServiceServer(new MockWebServiceMessageSender());
	}

	@Bean
	MockWebServiceServerWebServiceTemplateCustomizer mockWebServiceServerWebServiceTemplateCustomizer(
			TestMockWebServiceServer mockWebServiceServer) {
		return new MockWebServiceServerWebServiceTemplateCustomizer(mockWebServiceServer);
	}

}
