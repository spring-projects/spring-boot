/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.testing.springbootapplications.autoconfiguredwebservices.server

import javax.xml.transform.Source

import org.springframework.ws.server.endpoint.annotation.Endpoint
import org.springframework.ws.server.endpoint.annotation.PayloadRoot
import org.springframework.ws.server.endpoint.annotation.ResponsePayload
import org.springframework.xml.transform.StringSource

@Endpoint
class ExampleEndpoint {

	@PayloadRoot(localPart = "ExampleRequest")
	@ResponsePayload
	fun handleRequest(): Source {
		return StringSource("<ExampleResponse>42</ExampleResponse>")
	}

}

