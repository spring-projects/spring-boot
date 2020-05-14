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

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webservices.client.WebServiceTemplateBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.ws.test.client.MockWebServiceServer;
import org.springframework.xml.transform.StringSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.ws.test.client.RequestMatchers.payload;
import static org.springframework.ws.test.client.ResponseCreators.withPayload;

/**
 * Tests for {@link WebServiceClientTest @WebServiceClientTest} with no specific client.
 *
 * @author Dmytro Nosan
 */
@WebServiceClientTest
class WebServiceClientNoComponentIntegrationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private WebServiceTemplateBuilder webServiceTemplateBuilder;

	@Autowired
	private MockWebServiceServer server;

	@Test
	void exampleClientIsNotInjected() {
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
				.isThrownBy(() -> this.applicationContext.getBean(ExampleWebServiceClient.class));
	}

	@Test
	void manuallyCreateBean() {
		ExampleWebServiceClient client = new ExampleWebServiceClient(this.webServiceTemplateBuilder);
		this.server.expect(payload(new StringSource("<request/>")))
				.andRespond(withPayload(new StringSource("<response><status>200</status></response>")));
		assertThat(client.test()).extracting(Response::getStatus).isEqualTo(200);
	}

}
