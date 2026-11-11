/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webservices.test.autoconfigure.client;

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
