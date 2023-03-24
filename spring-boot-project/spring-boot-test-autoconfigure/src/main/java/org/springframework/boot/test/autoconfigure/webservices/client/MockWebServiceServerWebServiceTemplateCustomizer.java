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
