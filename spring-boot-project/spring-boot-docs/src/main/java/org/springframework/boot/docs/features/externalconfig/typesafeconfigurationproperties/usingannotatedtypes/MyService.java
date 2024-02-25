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

package org.springframework.boot.docs.features.externalconfig.typesafeconfigurationproperties.usingannotatedtypes;

import org.springframework.stereotype.Service;

/**
 * MyService class.
 */
@Service
public class MyService {

	private final MyProperties properties;

	/**
	 * Constructs a new instance of MyService with the specified properties.
	 * @param properties the properties to be used by the service
	 */
	public MyService(MyProperties properties) {
		this.properties = properties;
	}

	/**
	 * Opens a connection to the remote server.
	 *
	 * This method creates a new instance of the Server class using the remote address
	 * specified in the properties. It then starts the server, allowing it to accept
	 * incoming connections.
	 * @throws IOException if an I/O error occurs while opening the connection.
	 */
	public void openConnection() {
		Server server = new Server(this.properties.getRemoteAddress());
		server.start();
		// ...
	}

	// ...

}
