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
