/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.autoconfigure.webservices;

import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.ws.wsdl.wsdl11.SimpleWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebServicesAutoConfiguration}.
 *
 * @author Vedran Pavic
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Eneias Silva
 */
class WebServicesAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(WebServicesAutoConfiguration.class));

	@Test
	void defaultConfiguration() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(ServletRegistrationBean.class));
	}

	@Test
	void customPathMustBeginWithASlash() {
		this.contextRunner.withPropertyValues("spring.webservices.path=invalid")
			.run((context) -> assertThat(context).getFailure()
				.isInstanceOf(BeanCreationException.class)
				.rootCause()
				.hasMessageContaining("'path' must start with '/'"));
	}

	@Test
	void customPath() {
		this.contextRunner.withPropertyValues("spring.webservices.path=/valid")
			.run((context) -> assertThat(getUrlMappings(context)).contains("/valid/*"));
	}

	@Test
	void customPathWithTrailingSlash() {
		this.contextRunner.withPropertyValues("spring.webservices.path=/valid/")
			.run((context) -> assertThat(getUrlMappings(context)).contains("/valid/*"));
	}

	@Test
	void customLoadOnStartup() {
		this.contextRunner.withPropertyValues("spring.webservices.servlet.load-on-startup=1").run((context) -> {
			ServletRegistrationBean<?> registrationBean = context.getBean(ServletRegistrationBean.class);
			assertThat(registrationBean).extracting("loadOnStartup").isEqualTo(1);
		});
	}

	@Test
	void customInitParameters() {
		this.contextRunner
			.withPropertyValues("spring.webservices.servlet.init.key1=value1",
					"spring.webservices.servlet.init.key2=value2")
			.run((context) -> assertThat(getServletRegistrationBean(context).getInitParameters())
				.containsEntry("key1", "value1")
				.containsEntry("key2", "value2"));
	}

	@ParameterizedTest
	@WithResource(name = "wsdl/service.wsdl", content = """
			<?xml version="1.0" encoding="UTF-8"?>
			<wsdl:definitions
				xmlns:wsdl="http://schemas.xmlsoap.org/wsdl/"
				xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/"
				xmlns:tns="https://www.springframework.org/spring-ws/wsdl"
				targetNamespace="https://www.springframework.org/spring-ws/wsdl">
				<wsdl:types>
					<xsd:schema xmlns:xsd="http://www.w3.org/2001/XMLSchema"
						elementFormDefault="qualified"
						targetNamespace="https://www.springframework.org/spring-ws/wsdl">
						<xsd:element name="request" type="xsd:string" />
						<xsd:element name="response" type="xsd:string" />
					</xsd:schema>
				</wsdl:types>
				<wsdl:message name="responseMessage">
					<wsdl:part name="body" element="tns:response" />
				</wsdl:message>
				<wsdl:message name="requestMessage">
					<wsdl:part name="body" element="tns:request" />
				</wsdl:message>
				<wsdl:portType name="portType">
					<wsdl:operation name="operation">
						<wsdl:input message="tns:requestMessage" name="request" />
						<wsdl:output message="tns:responseMessage"
							name="response" />
					</wsdl:operation>
				</wsdl:portType>
				<wsdl:binding name="binding" type="tns:portType">
					<soap:binding style="document"
						transport="http://schemas.xmlsoap.org/soap/http" />
					<wsdl:operation name="operation">
						<soap:operation soapAction="" />
						<wsdl:input name="request">
							<soap:body use="literal" />
						</wsdl:input>
						<wsdl:output name="response">
							<soap:body use="literal" />
						</wsdl:output>
					</wsdl:operation>
				</wsdl:binding>
				<wsdl:service name="service">
					<wsdl:port binding="tns:binding" name="port">
						<soap:address location="/services" />
					</wsdl:port>
				</wsdl:service>
			</wsdl:definitions>
			""")
	@WithResource(name = "wsdl/types.xsd", content = """
			<?xml version="1.0" encoding="UTF-8"?>
			<schema xmlns="http://www.w3.org/2001/XMLSchema"
				elementFormDefault="qualified"
				targetNamespace="https://www.springframework.org/spring-ws/wsdl/schemas">
				<element name="request" type="string" />
				<element name="response" type="string" />
			</schema>
			""")
	@ValueSource(strings = { "spring.webservices.wsdl-locations", "spring.webservices.wsdl-locations[0]" })
	void withWsdlBeans(String propertyName) {
		this.contextRunner.withPropertyValues(propertyName + "=classpath:/wsdl").run((context) -> {
			assertThat(context.getBeansOfType(SimpleWsdl11Definition.class)).containsOnlyKeys("service");
			assertThat(context.getBeansOfType(SimpleXsdSchema.class)).containsOnlyKeys("types");
		});
	}

	private Collection<String> getUrlMappings(ApplicationContext context) {
		return getServletRegistrationBean(context).getUrlMappings();
	}

	private ServletRegistrationBean<?> getServletRegistrationBean(ApplicationContext loaded) {
		return loaded.getBean(ServletRegistrationBean.class);
	}

}
