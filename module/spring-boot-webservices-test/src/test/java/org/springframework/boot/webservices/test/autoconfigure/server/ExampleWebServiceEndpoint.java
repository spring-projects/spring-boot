/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webservices.test.autoconfigure.server;

import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

/**
 * Example web service {@code @Endpoint} used with
 * {@link WebServiceServerTest @WebServiceServerTest} tests.
 *
 * @author Daniil Razorenov
 */
@Endpoint
public class ExampleWebServiceEndpoint {

	@PayloadRoot(localPart = "request")
	@ResponsePayload
	Response payloadMethod(@RequestPayload Request request) {
		return new Response(42);
	}

}
