/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webservices.test.autoconfigure.client;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.boot.webservices.client.WebServiceTemplateBuilder;
import org.springframework.boot.webservices.client.WebServiceTemplateCustomizer;
import org.springframework.util.Assert;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.test.client.MockWebServiceServer;

/**
 * {@link WebServiceTemplateCustomizer} that can be applied to a
 * {@link WebServiceTemplateBuilder} instances to add {@link MockWebServiceServer}
 * support.
 *
 * @author Dmytro Nosan
 */
class MockWebServiceServerWebServiceTemplateCustomizer implements WebServiceTemplateCustomizer {

	private final AtomicBoolean applied = new AtomicBoolean();

	private final TestMockWebServiceServer mockServer;

	MockWebServiceServerWebServiceTemplateCustomizer(TestMockWebServiceServer mockServer) {
		this.mockServer = mockServer;
	}

	@Override
	public void customize(WebServiceTemplate webServiceTemplate) {
		Assert.state(!this.applied.getAndSet(true), "@WebServiceClientTest supports only a single WebServiceTemplate");
		webServiceTemplate.setMessageSender(this.mockServer.getMockMessageSender());
	}

}
