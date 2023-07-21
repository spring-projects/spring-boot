/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.test.autoconfigure.webservices.client;

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
