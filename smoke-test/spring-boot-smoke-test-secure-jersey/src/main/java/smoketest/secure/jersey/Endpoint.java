/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.secure.jersey;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.springframework.stereotype.Component;

@Component
@Path("/hello")
public class Endpoint {

	private final Service service;

	public Endpoint(Service service) {
		this.service = service;
	}

	@GET
	public String message() {
		return "Hello " + this.service.message();
	}

}
