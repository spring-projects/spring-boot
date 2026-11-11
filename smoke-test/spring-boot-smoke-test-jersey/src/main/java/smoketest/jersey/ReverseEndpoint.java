/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.jersey;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import org.springframework.stereotype.Component;

@Component
@Path("/reverse")
public class ReverseEndpoint {

	@GET
	public String reverse(@QueryParam("input") @NotNull String input) {
		return new StringBuilder(input).reverse().toString();
	}

}
