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

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for HTTP Interface Clients.
 * <p>
 * Allows using per-client properties or default if no client-specific found. Based on <a
 * href=https://github.com/spring-cloud/spring-cloud-commons/blob/main/spring-cloud-commons/src/main/java/org/springframework/cloud/client/loadbalancer/LoadBalancerClientsProperties.java>LoadBalancerClientsProperties.java</a>
 *
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
 * @since 3.4.0
 */
@ConfigurationProperties("spring.interfaceclients.http")
public class HttpInterfaceClientsProperties extends HttpInterfaceClientsBaseProperties {

	/**
	 * Client-specific interface client properties.
	 */
	private final Map<String, HttpInterfaceClientsBaseProperties> clients = new HashMap<>();

	public Map<String, HttpInterfaceClientsBaseProperties> getClients() {
		return this.clients;
	}

	public HttpInterfaceClientsBaseProperties getProperties(String clientId) {
		if (clientId == null || !this.getClients().containsKey(clientId)) {
			// no specific client properties, return default
			return this;
		}
		// because specifics are overlaid on top of defaults, everything in `properties`,
		// unless overridden, is in `clientsProperties`
		return this.getClients().get(clientId);
	}

}
