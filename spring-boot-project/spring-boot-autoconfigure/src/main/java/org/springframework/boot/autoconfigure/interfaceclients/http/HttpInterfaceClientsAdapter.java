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
import org.springframework.boot.autoconfigure.interfaceclients.InterfaceClientsAdapter;
import org.springframework.boot.autoconfigure.interfaceclients.QualifiedBeanProvider;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * HTTP-specific {@link InterfaceClientsAdapter} implementation.
 * <p>
 * Will attempt to use an {@link HttpServiceProxyFactory} provided by the user to create
 * an HTTP Interface Client. Beans qualified with a specific client id or
 * {@link InterfaceClientsAdapter#INTERFACE_CLIENTS_DEFAULT_QUALIFIER}) will be used. If
 * no user-provided bean is found, one with a default implementation is created.
 *
 * @author Josh Long
 * @author Olga Maciaszek-Sharma
 * @since 3.4.0
 */
public class HttpInterfaceClientsAdapter implements InterfaceClientsAdapter {

	private static final Log logger = LogFactory.getLog(HttpInterfaceClientsAdapter.class);

	private final HttpExchangeAdapterProvider adapterProvider;

	public HttpInterfaceClientsAdapter(HttpExchangeAdapterProvider adapterProvider) {
		this.adapterProvider = adapterProvider;
	}

	@Override
	public <T> T createClient(ListableBeanFactory beanFactory, String clientId, Class<T> type) {
		HttpServiceProxyFactory proxyFactory = proxyFactory(beanFactory, clientId);

		return proxyFactory.createClient(type);
	}

	private HttpServiceProxyFactory proxyFactory(ListableBeanFactory beanFactory, String clientId) {
		HttpServiceProxyFactory userProvidedProxyFactory = QualifiedBeanProvider.qualifiedBean(beanFactory,
				HttpServiceProxyFactory.class, clientId);
		if (userProvidedProxyFactory != null) {
			return userProvidedProxyFactory;
		}
		// create an HttpServiceProxyFactory bean with default implementation
		if (logger.isDebugEnabled()) {
			logger.debug("Creating HttpServiceProxyFactory for '" + clientId + "'");
		}
		HttpExchangeAdapter adapter = this.adapterProvider.get(beanFactory, clientId);
		return HttpServiceProxyFactory.builderFor(adapter).build();
	}

}
