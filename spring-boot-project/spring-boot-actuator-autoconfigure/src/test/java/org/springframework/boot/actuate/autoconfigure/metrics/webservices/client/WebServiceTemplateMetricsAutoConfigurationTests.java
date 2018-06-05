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

package org.springframework.boot.actuate.autoconfigure.metrics.webservices.client;

import javax.xml.bind.annotation.XmlRootElement;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.webservices.client.WebServiceTemplateAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.webservices.client.WebServiceTemplateBuilder;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.test.client.MockWebServiceServer;
import org.springframework.ws.test.client.RequestMatchers;
import org.springframework.ws.test.client.ResponseCreators;
import org.springframework.xml.transform.StringSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebServiceTemplateMetricsAutoConfiguration}.
 *
 * @author Dmytro Nosan
 */
public class WebServiceTemplateMetricsAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.with((context) -> context.withConfiguration(
					AutoConfigurations.of(SimpleMetricsExportAutoConfiguration.class,
							MetricsAutoConfiguration.class,
							CompositeMeterRegistryAutoConfiguration.class,
							WebServiceTemplateMetricsAutoConfiguration.class)))
			.withConfiguration(
					AutoConfigurations.of(WebServiceTemplateAutoConfiguration.class));

	@Test
	public void webServiceMetricsCustomizerWasApplied() {

		this.contextRunner.run((context) -> {
			MeterRegistry registry = context.getBean(MeterRegistry.class);
			WebServiceTemplate webServiceTemplate = context
					.getBean(WebServiceTemplateBuilder.class).build();

			Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
			marshaller.setClassesToBeBound(Request.class, Response.class);
			webServiceTemplate.setMarshaller(marshaller);
			webServiceTemplate.setUnmarshaller(marshaller);

			MockWebServiceServer mockServer = MockWebServiceServer
					.createServer(webServiceTemplate);

			mockServer.expect(RequestMatchers.payload(new StringSource("<request/>")))
					.andRespond(ResponseCreators
							.withPayload(new StringSource("<response/>")));

			assertThat(registry.find("webservices.client.requests").meter()).isNull();
			webServiceTemplate.marshalSendAndReceive("http://localhost:8080",
					new Request());
			assertThat(registry.get("webservices.client.requests").meter()).isNotNull();

			mockServer.verify();

		});
	}

	@XmlRootElement(name = "request")
	static class Request {

	}

	@XmlRootElement(name = "response")
	static class Response {

	}

}
