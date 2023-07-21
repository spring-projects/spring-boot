/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.observation;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for configuring Micrometer
 * observations.
 *
 * @author Brian Clozel
 * @author Moritz Halbritter
 * @since 3.0.0
 */
@ConfigurationProperties("management.observations")
public class ObservationProperties {

	private final Http http = new Http();

	/**
	 * Common key-values that are applied to every observation.
	 */
	private Map<String, String> keyValues = new LinkedHashMap<>();

	/**
	 * Whether observations starting with the specified name should be enabled. The
	 * longest match wins, the key 'all' can also be used to configure all observations.
	 */
	private Map<String, Boolean> enable = new LinkedHashMap<>();

	public Map<String, Boolean> getEnable() {
		return this.enable;
	}

	public void setEnable(Map<String, Boolean> enable) {
		this.enable = enable;
	}

	public Http getHttp() {
		return this.http;
	}

	public Map<String, String> getKeyValues() {
		return this.keyValues;
	}

	public void setKeyValues(Map<String, String> keyValues) {
		this.keyValues = keyValues;
	}

	public static class Http {

		private final Client client = new Client();

		private final Server server = new Server();

		public Client getClient() {
			return this.client;
		}

		public Server getServer() {
			return this.server;
		}

		public static class Client {

			private final ClientRequests requests = new ClientRequests();

			public ClientRequests getRequests() {
				return this.requests;
			}

			public static class ClientRequests {

				/**
				 * Name of the observation for client requests.
				 */
				private String name = "http.client.requests";

				public String getName() {
					return this.name;
				}

				public void setName(String name) {
					this.name = name;
				}

			}

		}

		public static class Server {

			private final ServerRequests requests = new ServerRequests();

			public ServerRequests getRequests() {
				return this.requests;
			}

			public static class ServerRequests {

				/**
				 * Name of the observation for server requests.
				 */
				private String name = "http.server.requests";

				public String getName() {
					return this.name;
				}

				public void setName(String name) {
					this.name = name;
				}

			}

		}

	}

}
