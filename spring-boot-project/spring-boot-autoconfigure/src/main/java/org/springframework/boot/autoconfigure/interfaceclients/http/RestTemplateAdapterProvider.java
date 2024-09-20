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

package org.springframework.boot.autoconfigure.interfaceclients.http;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.interfaceclients.InterfaceClientsAdapter;
import org.springframework.boot.autoconfigure.interfaceclients.QualifiedBeanProvider;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.support.RestTemplateAdapter;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.util.DefaultUriBuilderFactory;

/**
 * {@link RestTemplate}-backed {@link HttpExchangeAdapterProvider} implementation.
 * <p>
 * Will attempt to use a {@link RestTemplate} or {@link RestTemplateBuilder} bean provided
 * by the user to create a {@link RestTemplateAdapter}. Beans qualified with a specific
 * client id or {@link InterfaceClientsAdapter#INTERFACE_CLIENTS_DEFAULT_QUALIFIER}) will
 * be used. If no user-provided bean is found, one with a default implementation is
 * created.
 *
 * @author Olga Maciaszek-Sharma
 * @since 3.4.0
 */
public class RestTemplateAdapterProvider implements HttpExchangeAdapterProvider {

	private static final Log logger = LogFactory.getLog(RestTemplateAdapterProvider.class);

	private final ObjectProvider<RestTemplateBuilder> restTemplateBuilderProvider;

	private final ObjectProvider<HttpInterfaceClientsProperties> propertiesProvider;

	public RestTemplateAdapterProvider(ObjectProvider<RestTemplateBuilder> restTemplateBuilderProvider,
			ObjectProvider<HttpInterfaceClientsProperties> propertiesProvider) {
		this.restTemplateBuilderProvider = restTemplateBuilderProvider;
		this.propertiesProvider = propertiesProvider;
	}

	@Override
	public HttpExchangeAdapter get(ListableBeanFactory beanFactory, String clientId) {
		HttpInterfaceClientsProperties properties = this.propertiesProvider.getObject();
		String baseUrl = properties.getProperties(clientId).getBaseUrl();

		RestTemplate userProvidedRestTemplate = QualifiedBeanProvider.qualifiedBean(beanFactory, RestTemplate.class,
				clientId);
		if (userProvidedRestTemplate != null) {
			// If the user wants to set the baseUrl directly on the builder,
			// it should not be set in the properties.
			if (baseUrl != null) {
				userProvidedRestTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(baseUrl));
			}
			return RestTemplateAdapter.create(userProvidedRestTemplate);
		}

		RestTemplateBuilder userProvidedRestTemplateBuilder = QualifiedBeanProvider.qualifiedBean(beanFactory,
				RestTemplateBuilder.class, clientId);
		if (userProvidedRestTemplateBuilder != null) {
			// If the user wants to set the baseUrl directly on the builder,
			// it should not be set in the properties.
			if (baseUrl != null) {
				userProvidedRestTemplateBuilder.rootUri(baseUrl);
			}
			return RestTemplateAdapter.create(userProvidedRestTemplateBuilder.build());
		}

		// create a RestTemplateAdapter bean with default implementation
		if (logger.isDebugEnabled()) {
			logger.debug("Creating RestTemplateAdapter for '" + clientId + "'");
		}
		RestTemplate restTemplate = this.restTemplateBuilderProvider.getObject().rootUri(baseUrl).build();
		return RestTemplateAdapter.create(restTemplate);
	}

}
