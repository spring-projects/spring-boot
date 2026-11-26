/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.jersey.jackson2;

import org.springframework.boot.actuate.endpoint.OperationResponseBody;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

/**
 * Names endpoint.
 *
 * @author Kristoffer Larsen Hopland
 */
@Component
@Endpoint(id = "names")
public class NamesEndpoint {

	@ReadOperation
	NamesDescriptor names() {
		return new NamesDescriptor("Spring", "Boot");
	}

	/**
	 * Descriptor for the names endpoint.
	 *
	 * @param firstName the first name
	 * @param lastName the last name
	 */
	public record NamesDescriptor(String firstName, String lastName) implements OperationResponseBody {

	}

}
