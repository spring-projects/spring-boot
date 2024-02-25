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

package smoketest.jersey;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.springframework.stereotype.Component;

/**
 * Endpoint class.
 */
@Component
@Path("/hello")
public class Endpoint {

	private final Service service;

	/**
	 * Constructs a new Endpoint object with the specified Service.
	 * @param service the Service object to be associated with the Endpoint
	 */
	public Endpoint(Service service) {
		this.service = service;
	}

	/**
	 * Retrieves a message from the service and returns it.
	 * @return the message retrieved from the service
	 */
	@GET
	public String message() {
		return "Hello " + this.service.message();
	}

}
