/*
 * Copyright 2012-2022 the original author or authors.
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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for configuring Micrometer
 * observations.
 *
 * @author Brian Clozel
 * @since 3.0.0
 */
@ConfigurationProperties("management.observations")
public class ObservationProperties {

	private final Http http = new Http();

	public Http getHttp() {
		return this.http;
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
				 * Name of the observation for client requests. If empty, will use the
				 * default "http.client.requests".
				 */
				private String name;

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
				 * Name of the observation for server requests. If empty, will use the
				 * default "http.server.requests".
				 */
				private String name;

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
