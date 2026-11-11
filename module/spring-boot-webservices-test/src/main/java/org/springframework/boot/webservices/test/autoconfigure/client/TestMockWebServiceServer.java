/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webservices.test.autoconfigure.client;

import org.springframework.ws.test.client.MockWebServiceMessageSender;
import org.springframework.ws.test.client.MockWebServiceServer;

/**
 * Test {@link MockWebServiceServer} which provides access to the underlying
 * {@link MockWebServiceMessageSender}.
 *
 * @author Dmytro Nosan
 */
final class TestMockWebServiceServer extends MockWebServiceServer {

	private final MockWebServiceMessageSender mockMessageSender;

	TestMockWebServiceServer(MockWebServiceMessageSender mockMessageSender) {
		super(mockMessageSender);
		this.mockMessageSender = mockMessageSender;
	}

	MockWebServiceMessageSender getMockMessageSender() {
		return this.mockMessageSender;
	}

}
