/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.metrics.webservices.client;

import javax.xml.bind.annotation.XmlRootElement;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.WebServiceIOException;
import org.springframework.ws.client.WebServiceTransformerException;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.SoapElementException;
import org.springframework.ws.soap.client.core.SoapActionCallback;
import org.springframework.ws.test.client.MockWebServiceServer;
import org.springframework.ws.test.client.RequestMatchers;
import org.springframework.ws.test.client.ResponseCreators;
import org.springframework.xml.transform.StringSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MetricsWebServiceTemplateCustomizer}.
 *
 * @author Dmytro Nosan
 */
public class MetricsWebServiceTemplateCustomizerTests {

	private MeterRegistry registry;

	private WebServiceTemplate webServiceTemplate;

	private MockWebServiceServer mockServer;

	private MetricsWebServiceTemplateCustomizer customizer;

	@Before
	public void setup() {
		this.registry = new SimpleMeterRegistry(SimpleConfig.DEFAULT, new MockClock());
		this.webServiceTemplate = new WebServiceTemplate();
		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		marshaller.setClassesToBeBound(Request.class, Response.class);
		this.webServiceTemplate.setMarshaller(marshaller);
		this.webServiceTemplate.setUnmarshaller(marshaller);
		this.mockServer = MockWebServiceServer.createServer(this.webServiceTemplate);
		this.customizer = new MetricsWebServiceTemplateCustomizer(this.registry,
				new DefaultWebServiceTemplateExchangeTagsProvider(),
				"webservices.client.requests");
		this.customizer.customize(this.webServiceTemplate);
	}

	@After
	public void tearDown() {
		if (this.mockServer != null) {
			this.mockServer.verify();
		}
	}

	@Test
	public void interceptWebServiceTemplate() {

		this.mockServer.expect(RequestMatchers.payload(new StringSource("<request/>")))
				.andRespond(
						ResponseCreators.withPayload(new StringSource("<response/>")));

		this.webServiceTemplate.marshalSendAndReceive("http://localhost:8080/test",
				new Request());

		assertThat(this.registry.get("webservices.client.requests")
				.tags("uri", "/test", "uri", "/test", "status", "OK").timer().count())
						.isEqualTo(1);
	}

	@Test
	public void avoidDuplicateRegistration() {
		this.customizer.customize(this.webServiceTemplate);
		assertThat(this.webServiceTemplate.getInterceptors()).hasSize(1);
		this.customizer.customize(this.webServiceTemplate);
		assertThat(this.webServiceTemplate.getInterceptors()).hasSize(1);
	}

	@Test
	public void normalizeUriToContainLeadingSlash() {

		this.mockServer.expect(RequestMatchers.payload(new StringSource("<request/>")))
				.andRespond(
						ResponseCreators.withPayload(new StringSource("<response/>")));

		this.webServiceTemplate.marshalSendAndReceive("http://localhost:8080/test",
				new Request());

		this.registry.get("webservices.client.requests").tags("uri", "/test").timer();

	}

	@Test
	public void plainAction() {

		this.mockServer.expect(RequestMatchers.payload(new StringSource("<request/>")))
				.andRespond(
						ResponseCreators.withPayload(new StringSource("<response/>")));

		this.webServiceTemplate.marshalSendAndReceive("http://localhost:8080/test",
				new Request(), new SoapActionCallback("test"));

		this.registry.get("webservices.client.requests").tags("action", "test").timer();

	}

	@Test
	public void extensionAction() {

		this.mockServer.expect(RequestMatchers.payload(new StringSource("<request/>")))
				.andRespond(
						ResponseCreators.withPayload(new StringSource("<response/>")));

		this.webServiceTemplate.marshalSendAndReceive("http://localhost:8080/test",
				new Request(), new SoapActionCallback("myapp.sdl"));

		this.registry.get("webservices.client.requests").tags("action", "myapp.sdl")
				.timer();

	}

	@Test
	public void uriAction() {

		this.mockServer.expect(RequestMatchers.payload(new StringSource("<request/>")))
				.andRespond(
						ResponseCreators.withPayload(new StringSource("<response/>")));

		this.webServiceTemplate.marshalSendAndReceive("http://localhost:8080/test",
				new Request(),
				new SoapActionCallback("http://electrocommerce.org/abc#MyMessage"));

		this.registry.get("webservices.client.requests").tags("action", "/abc#MyMessage")
				.timer();

	}

	@Test
	public void emptyAction() {

		this.mockServer.expect(RequestMatchers.payload(new StringSource("<request/>")))
				.andRespond(
						ResponseCreators.withPayload(new StringSource("<response/>")));

		this.webServiceTemplate.marshalSendAndReceive("http://localhost:8080/test",
				new Request(), new SoapActionCallback(""));

		this.registry.get("webservices.client.requests").tags("action", "NONE").timer();

	}

	@Test
	public void quotedAction() {

		this.mockServer.expect(RequestMatchers.payload(new StringSource("<request/>")))
				.andRespond(
						ResponseCreators.withPayload(new StringSource("<response/>")));

		this.webServiceTemplate.marshalSendAndReceive("http://localhost:8080/test",
				new Request(), new SoapActionCallback("\"\""));

		this.registry.get("webservices.client.requests").tags("action", "NONE").timer();

	}

	@Test
	public void okStatus() {

		this.mockServer.expect(RequestMatchers.payload(new StringSource("<request/>")))
				.andRespond(
						ResponseCreators.withPayload(new StringSource("<response/>")));

		this.webServiceTemplate.marshalSendAndReceive("http://localhost:8080/test",
				new Request());

		this.registry.get("webservices.client.requests").tags("status", "OK").timer();

	}

	@Test
	public void clientErrorStatus() {
		this.mockServer.expect(RequestMatchers.payload(new StringSource("<request/>")))
				.andRespond((uri, request, messageFactory) -> {
					throw new WebServiceTransformerException("");
				});
		try {
			this.webServiceTemplate.marshalSendAndReceive("http://localhost:8080/test",
					new Request());
		}
		catch (WebServiceTransformerException ignore) {
		}
		this.registry.get("webservices.client.requests").tags("status", "CLIENT_ERROR")
				.timer();

	}

	@Test
	public void ioErrorStatus() {

		this.mockServer.expect(RequestMatchers.payload(new StringSource("<request/>")))
				.andRespond((uri, request, messageFactory) -> {
					throw new WebServiceIOException("");
				});
		try {

			this.webServiceTemplate.marshalSendAndReceive("http://localhost:8080/test",
					new Request());
		}
		catch (WebServiceIOException ignore) {
		}

		this.registry.get("webservices.client.requests").tags("status", "IO_ERROR")
				.timer();

	}

	@Test
	public void serverErrorStatus() {

		this.mockServer.expect(RequestMatchers.payload(new StringSource("<request/>")))
				.andRespond((uri, request, messageFactory) -> {
					throw new SoapElementException("");
				});
		try {

			this.webServiceTemplate.marshalSendAndReceive("http://localhost:8080/test",
					new Request());
		}
		catch (SoapElementException ignore) {
		}

		this.registry.get("webservices.client.requests").tags("status", "SERVER_ERROR")
				.timer();

	}

	@XmlRootElement(name = "request")
	static class Request {

	}

	@XmlRootElement(name = "response")
	static class Response {

	}

}
