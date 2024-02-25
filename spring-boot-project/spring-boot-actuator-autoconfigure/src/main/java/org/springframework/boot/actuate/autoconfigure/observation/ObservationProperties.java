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

	/**
     * Returns a map of enable values.
     * 
     * @return a map containing enable values as Boolean objects
     */
    public Map<String, Boolean> getEnable() {
		return this.enable;
	}

	/**
     * Sets the enable status for each observation property.
     * 
     * @param enable a map containing the enable status for each observation property. The key is the property name and the value is a boolean indicating whether the property is enabled or not.
     */
    public void setEnable(Map<String, Boolean> enable) {
		this.enable = enable;
	}

	/**
     * Returns the Http object associated with this ObservationProperties instance.
     *
     * @return the Http object
     */
    public Http getHttp() {
		return this.http;
	}

	/**
     * Returns the key-value pairs stored in the ObservationProperties object.
     * 
     * @return a Map containing the key-value pairs
     */
    public Map<String, String> getKeyValues() {
		return this.keyValues;
	}

	/**
     * Sets the key-value pairs for the ObservationProperties.
     * 
     * @param keyValues the key-value pairs to be set
     */
    public void setKeyValues(Map<String, String> keyValues) {
		this.keyValues = keyValues;
	}

	/**
     * Http class.
     */
    public static class Http {

		private final Client client = new Client();

		private final Server server = new Server();

		/**
         * Returns the client associated with this Http instance.
         *
         * @return the client associated with this Http instance
         */
        public Client getClient() {
			return this.client;
		}

		/**
         * Returns the server object associated with this Http instance.
         *
         * @return the server object
         */
        public Server getServer() {
			return this.server;
		}

		/**
         * Client class.
         */
        public static class Client {

			private final ClientRequests requests = new ClientRequests();

			/**
             * Returns the ClientRequests object containing the requests made by the client.
             *
             * @return the ClientRequests object containing the requests made by the client
             */
            public ClientRequests getRequests() {
				return this.requests;
			}

			/**
             * ClientRequests class.
             */
            public static class ClientRequests {

				/**
				 * Name of the observation for client requests.
				 */
				private String name = "http.client.requests";

				/**
                 * Returns the name of the client.
                 *
                 * @return the name of the client
                 */
                public String getName() {
					return this.name;
				}

				/**
                 * Sets the name of the client.
                 * 
                 * @param name the name of the client
                 */
                public void setName(String name) {
					this.name = name;
				}

			}

		}

		/**
         * Server class.
         */
        public static class Server {

			private final ServerRequests requests = new ServerRequests();

			/**
             * Returns the ServerRequests object that contains the requests made to the server.
             *
             * @return the ServerRequests object containing the requests made to the server
             */
            public ServerRequests getRequests() {
				return this.requests;
			}

			/**
             * ServerRequests class.
             */
            public static class ServerRequests {

				/**
				 * Name of the observation for server requests.
				 */
				private String name = "http.server.requests";

				/**
                 * Returns the name of the ServerRequests object.
                 *
                 * @return the name of the ServerRequests object
                 */
                public String getName() {
					return this.name;
				}

				/**
                 * Sets the name of the server request.
                 * 
                 * @param name the name to be set for the server request
                 */
                public void setName(String name) {
					this.name = name;
				}

			}

		}

	}

}
