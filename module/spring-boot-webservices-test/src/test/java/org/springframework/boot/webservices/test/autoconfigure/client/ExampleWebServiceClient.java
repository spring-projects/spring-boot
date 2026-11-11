/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webservices.test.autoconfigure.client;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.webservices.client.WebServiceTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.ws.client.core.WebServiceTemplate;

/**
 * Example web client used with {@link WebServiceClientTest @WebServiceClientTest} tests.
 *
 * @author Dmytro Nosan
 */
@Service
public class ExampleWebServiceClient {

	private final WebServiceTemplate webServiceTemplate;

	public ExampleWebServiceClient(WebServiceTemplateBuilder builder) {
		this.webServiceTemplate = builder.build();
	}

	public @Nullable Response test() {
		return (Response) this.webServiceTemplate.marshalSendAndReceive("https://example.com", new Request());
	}

}
