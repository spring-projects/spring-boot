/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.docs.io.webservices.template;

import java.time.Duration;

import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.webservices.client.WebServiceMessageSenderFactory;
import org.springframework.boot.webservices.client.WebServiceTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ws.client.core.WebServiceTemplate;

@Configuration(proxyBeanMethods = false)
public class MyWebServiceTemplateConfiguration {

	@Bean
	public WebServiceTemplate webServiceTemplate(WebServiceTemplateBuilder builder) {
		ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
			.withConnectTimeout(Duration.ofSeconds(2))
			.withReadTimeout(Duration.ofSeconds(2));
		builder.httpMessageSenderFactory(WebServiceMessageSenderFactory.http(settings));
		return builder.build();
	}

}
