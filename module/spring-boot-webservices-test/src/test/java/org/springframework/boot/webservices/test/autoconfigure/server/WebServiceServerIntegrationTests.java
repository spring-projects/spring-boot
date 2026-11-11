/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webservices.test.autoconfigure.server;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ws.test.server.MockWebServiceClient;
import org.springframework.ws.test.server.RequestCreators;
import org.springframework.ws.test.server.ResponseMatchers;
import org.springframework.xml.transform.StringSource;

/**
 * Tests for {@link WebServiceServerTest @WebServiceServerTest}.
 *
 * @author Daniil Razorenov
 */
@WebServiceServerTest(endpoints = ExampleWebServiceEndpoint.class)
class WebServiceServerIntegrationTests {

	@Autowired
	private MockWebServiceClient mock;

	@Test
	void payloadRootMethod() {
		this.mock
			.sendRequest(RequestCreators.withPayload(new StringSource("<request><message>Hello</message></request>")))
			.andExpect(ResponseMatchers.payload(new StringSource("<response><code>42</code></response>")));
	}

}
