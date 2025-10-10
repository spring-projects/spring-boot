/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.webservices.autoconfigure.client;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.webservices.client.WebServiceMessageSenderFactory;
import org.springframework.boot.webservices.client.WebServiceTemplateBuilder;
import org.springframework.boot.webservices.client.WebServiceTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.ws.client.core.WebServiceTemplate;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link WebServiceTemplate}.
 *
 * @author Dmytro Nosan
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass({ WebServiceTemplate.class, Unmarshaller.class, Marshaller.class })
public final class WebServiceTemplateAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	WebServiceMessageSenderFactory webServiceHttpMessageSenderFactory(
			ObjectProvider<ClientHttpRequestFactoryBuilder<?>> clientHttpRequestFactoryBuilder,
			ObjectProvider<HttpClientSettings> httpClientSettings) {
		return WebServiceMessageSenderFactory.http(
				clientHttpRequestFactoryBuilder.getIfAvailable(ClientHttpRequestFactoryBuilder::detect),
				httpClientSettings.getIfAvailable());
	}

	@Bean
	@ConditionalOnMissingBean
	WebServiceTemplateBuilder webServiceTemplateBuilder(
			ObjectProvider<WebServiceMessageSenderFactory> httpWebServiceMessageSenderBuilder,
			ObjectProvider<WebServiceTemplateCustomizer> webServiceTemplateCustomizers) {
		WebServiceTemplateBuilder templateBuilder = new WebServiceTemplateBuilder();
		WebServiceMessageSenderFactory httpMessageSenderFactory = httpWebServiceMessageSenderBuilder.getIfAvailable();
		if (httpMessageSenderFactory != null) {
			templateBuilder = templateBuilder.httpMessageSenderFactory(httpMessageSenderFactory);
		}
		List<WebServiceTemplateCustomizer> customizers = webServiceTemplateCustomizers.orderedStream().toList();
		if (!customizers.isEmpty()) {
			templateBuilder = templateBuilder.customizers(customizers);
		}
		return templateBuilder;
	}

}
