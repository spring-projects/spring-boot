/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.testing.springbootapplications.autoconfiguredwebservices.client;

import org.springframework.boot.webservices.client.WebServiceTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.ws.client.core.WebServiceTemplate;

@Service
public class SomeWebService {

	private final WebServiceTemplate webServiceTemplate;

	public SomeWebService(WebServiceTemplateBuilder builder) {
		this.webServiceTemplate = builder.build();
	}

	public Response test() {
		return (Response) this.webServiceTemplate.marshalSendAndReceive("https://example.com", new Request());
	}

}
