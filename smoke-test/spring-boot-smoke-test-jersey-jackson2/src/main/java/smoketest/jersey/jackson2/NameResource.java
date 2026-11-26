/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.jersey.jackson2;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.springframework.stereotype.Component;

/**
 * Jersey resource that returns a {@link Name}.
 *
 * @author Kristoffer Larsen Hopland
 */
@Component
@Path("/name")
public class NameResource {

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Name name() {
		return new Name("Spring", "Boot");
	}

}
