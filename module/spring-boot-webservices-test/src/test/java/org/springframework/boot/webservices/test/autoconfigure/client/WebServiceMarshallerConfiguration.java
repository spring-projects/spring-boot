/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webservices.test.autoconfigure.client;

import org.springframework.boot.webservices.client.WebServiceTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

/**
 * Test configuration to configure {@code Marshaller} and {@code Unmarshaller}.
 *
 * @author Dmytro Nosan
 */
@Configuration(proxyBeanMethods = false)
class WebServiceMarshallerConfiguration {

	@Bean
	WebServiceTemplateCustomizer marshallerCustomizer(Marshaller marshaller) {
		return (webServiceTemplate) -> webServiceTemplate.setMarshaller(marshaller);
	}

	@Bean
	WebServiceTemplateCustomizer unmarshallerCustomizer(Unmarshaller unmarshaller) {
		return (webServiceTemplate) -> webServiceTemplate.setUnmarshaller(unmarshaller);
	}

	@Bean
	Jaxb2Marshaller createJaxbMarshaller() {
		Jaxb2Marshaller jaxb2Marshaller = new Jaxb2Marshaller();
		jaxb2Marshaller.setClassesToBeBound(Request.class, Response.class);
		return jaxb2Marshaller;
	}

}
