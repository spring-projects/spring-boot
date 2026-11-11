/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webservices.test.autoconfigure.client;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ws.client.WebServiceTransportException;
import org.springframework.ws.test.client.MockWebServiceServer;
import org.springframework.ws.test.support.SourceAssertionError;
import org.springframework.xml.transform.StringSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.ws.test.client.RequestMatchers.connectionTo;
import static org.springframework.ws.test.client.RequestMatchers.payload;
import static org.springframework.ws.test.client.ResponseCreators.withError;
import static org.springframework.ws.test.client.ResponseCreators.withPayload;

/**
 * Tests for {@link WebServiceClientTest @WebServiceClientTest}.
 *
 * @author Dmytro Nosan
 */
@WebServiceClientTest(ExampleWebServiceClient.class)
class WebServiceClientIntegrationTests {

	@Autowired
	private MockWebServiceServer server;

	@Autowired
	private ExampleWebServiceClient client;

	@Test
	void mockServerCall() {
		this.server.expect(payload(new StringSource("<request/>")))
			.andRespond(withPayload(new StringSource("<response><status>200</status></response>")));
		assertThat(this.client.test()).extracting(Response::getStatus).isEqualTo(200);
	}

	@Test
	void mockServerCall1() {
		this.server.expect(connectionTo("https://example1")).andRespond(withPayload(new StringSource("<response/>")));
		assertThatExceptionOfType(SourceAssertionError.class).isThrownBy(this.client::test)
			.withMessageContaining("Unexpected connection expected");
	}

	@Test
	void mockServerCall2() {
		this.server.expect(payload(new StringSource("<request/>"))).andRespond(withError("Invalid Request"));
		assertThatExceptionOfType(WebServiceTransportException.class).isThrownBy(this.client::test)
			.withMessageContaining("Invalid Request");
	}

}
