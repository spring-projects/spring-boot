/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.webservices.server;

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
