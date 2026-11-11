/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webservices.test.autoconfigure.client;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.ws.test.client.MockWebServiceServer;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link AutoConfigureMockWebServiceServer @AutoConfigureMockWebServiceServer}
 * with {@code enabled=false}.
 *
 * @author Dmytro Nosan
 */
@WebServiceClientTest
@AutoConfigureMockWebServiceServer(enabled = false)
class AutoConfigureMockWebServiceServerEnabledIntegrationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void mockWebServiceServerShouldNotBeRegistered() {
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
			.isThrownBy(() -> this.applicationContext.getBean(MockWebServiceServer.class));
	}

}
